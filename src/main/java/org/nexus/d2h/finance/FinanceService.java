package org.nexus.d2h.finance;

import lombok.extern.slf4j.Slf4j;
import org.nexus.d2h.boxsale.StbSale;
import org.nexus.d2h.common.BusinessException;
import org.nexus.d2h.common.PageResponse;
import org.nexus.d2h.common.ResourceNotFoundException;
import org.nexus.d2h.audit.AuditService;
import org.nexus.d2h.notification.NotificationEventPublisher;
import org.nexus.d2h.notification.NotificationEventType;
import org.nexus.d2h.retailer.Retailer;
import org.nexus.d2h.retailer.RetailerRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Slf4j
@Service
public class FinanceService {

    private final FinancialTransactionRepository txRepository;
    private final RetailerRepository retailerRepository;
    private final NotificationEventPublisher eventPublisher;
    private final AuditService auditService;

    public FinanceService(FinancialTransactionRepository txRepository,
                          RetailerRepository retailerRepository,
                          NotificationEventPublisher eventPublisher,
                          AuditService auditService) {
        this.txRepository = txRepository;
        this.retailerRepository = retailerRepository;
        this.eventPublisher = eventPublisher;
        this.auditService = auditService;
    }

    // ── Manual transaction creation ───────────────────────────────────────────

    @Transactional
    public FinancialTransactionDto create(CreateTransactionRequest request) {
        Retailer retailer = retailerRepository.findById(request.retailerId())
                .orElseThrow(() -> new ResourceNotFoundException("Retailer", request.retailerId()));

        String ref = resolveReference(request.reference());

        FinancialTransaction tx = buildTransaction(retailer, request.transactionType(),
                request.transactionDate(), request.amount(), request.paymentMethod(),
                ref, request.paymentReference(), request.description(), request.remarks(),
                TransactionSource.MANUAL, null);

        FinancialTransaction saved = txRepository.save(tx);
        log.info("Finance tx created: id={} type={} amount={} retailer={}",
                saved.getId(), saved.getTransactionType(), saved.getAmount(),
                retailer.getRetailerCode());
        publishFinanceEvent(NotificationEventType.FINANCE_TRANSACTION_CREATED, saved);
        auditService.record("FinancialTransaction", String.valueOf(saved.getId()),
                "CREATE", "type=" + saved.getTransactionType() + " amount=" + saved.getAmount()
                        + " retailer=" + retailer.getRetailerCode(), null);
        return FinancialTransactionDto.from(saved);
    }

    @Transactional
    public void recordBoxSale(Retailer retailer, StbSale sale) {
        if (txRepository.existsBySaleId(sale.getId())) {
            log.warn("Finance record for sale {} already exists — skipping", sale.getId());
            return;
        }
        String ref = "SALE-" + sale.getId();
        if (txRepository.existsByReference(ref)) {
            ref = "SALE-" + sale.getId() + "-" + UUID.randomUUID().toString().substring(0, 8);
        }
        FinancialTransaction tx = buildTransaction(retailer, TransactionType.BOX_SALE,
                sale.getTransactionDate(), sale.getTotalAmount(), null,
                ref, null, "Box sale #" + sale.getId(), null,
                TransactionSource.SYSTEM, sale);
        txRepository.save(tx);
        log.info("BOX_SALE finance tx created for sale={} amount={}", sale.getId(), sale.getTotalAmount());
    }

    // ── Read ──────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public FinancialTransactionDto getById(Long id) {
        return FinancialTransactionDto.from(txRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("FinancialTransaction", id)));
    }

    @Transactional(readOnly = true)
    public PageResponse<FinancialTransactionDto> search(
            Long retailerId, TransactionType type, TransactionStatus status,
            PaymentMethod paymentMethod, LocalDate dateFrom, LocalDate dateTo,
            String reference, BigDecimal amountMin, BigDecimal amountMax,
            Pageable pageable) {

        Page<FinancialTransactionDto> page = txRepository.findAll(
                FinancialTransactionSpecification.search(retailerId, type, status,
                        paymentMethod, dateFrom, dateTo, reference, amountMin, amountMax),
                pageable).map(FinancialTransactionDto::from);
        return PageResponse.from(page);
    }

    // ── Reversal ──────────────────────────────────────────────────────────────

    @Transactional
    public FinancialTransactionDto reverse(Long id, String reason) {
        FinancialTransaction original = txRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("FinancialTransaction", id));

        if (original.getReversedBy() != null) {
            throw new BusinessException("ALREADY_REVERSED", "Transaction has already been reversed");
        }
        if (original.getTransactionStatus() != TransactionStatus.POSTED) {
            throw new BusinessException("TRANSACTION_NOT_REVERSIBLE",
                    "Only POSTED transactions can be reversed");
        }
        if (original.getTransactionType() == TransactionType.REVERSAL) {
            throw new BusinessException("TRANSACTION_NOT_REVERSIBLE",
                    "A reversal transaction cannot be reversed");
        }

        String username = currentUsername();
        String reversalRef = "REV-" + original.getId() + "-" + UUID.randomUUID().toString().substring(0, 8);

        FinancialTransaction reversal = buildTransaction(
                original.getRetailer(), TransactionType.REVERSAL,
                LocalDate.now(), original.getAmount().negate(),
                original.getPaymentMethod(), reversalRef,
                original.getPaymentReference(),
                "Reversal of transaction #" + original.getId() + (reason != null ? ": " + reason : ""),
                reason, TransactionSource.MANUAL, null);
        reversal.setReversalOf(original);
        FinancialTransaction savedReversal = txRepository.save(reversal);

        original.setTransactionStatus(TransactionStatus.REVERSED);
        original.setReversedBy(savedReversal);
        original.setUpdatedBy(username);
        txRepository.save(original);

        log.info("Transaction {} reversed by {} — reversal id={}", id, username, savedReversal.getId());
        publishFinanceEvent(NotificationEventType.FINANCE_TRANSACTION_REVERSED, savedReversal);
        auditService.record("FinancialTransaction", String.valueOf(id),
                "REVERSE", "reversalId=" + savedReversal.getId(), null);
        return FinancialTransactionDto.from(savedReversal);
    }

    // ── Adjustment ────────────────────────────────────────────────────────────

    @Transactional
    public FinancialTransactionDto adjust(Long id, AdjustTransactionRequest request) {
        FinancialTransaction original = txRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("FinancialTransaction", id));

        if (original.getTransactionStatus() != TransactionStatus.POSTED) {
            throw new BusinessException("TRANSACTION_NOT_ADJUSTABLE",
                    "Only POSTED transactions can be adjusted");
        }
        if (request.adjustmentAmount().compareTo(BigDecimal.ZERO) == 0) {
            throw new BusinessException("ZERO_ADJUSTMENT", "Adjustment amount must not be zero");
        }

        String adjustRef = "ADJ-" + original.getId() + "-" + UUID.randomUUID().toString().substring(0, 8);

        FinancialTransaction adjustment = buildTransaction(
                original.getRetailer(), TransactionType.ADJUSTMENT,
                LocalDate.now(), request.adjustmentAmount(),
                original.getPaymentMethod(), adjustRef, null,
                "Adjustment of transaction #" + original.getId() + ": " + request.reason(),
                request.reason(), TransactionSource.MANUAL, null);
        adjustment.setReversalOf(original);
        FinancialTransaction saved = txRepository.save(adjustment);

        log.info("Adjustment {} created for transaction {} amount={}", saved.getId(), id, request.adjustmentAmount());
        publishFinanceEvent(NotificationEventType.FINANCE_TRANSACTION_ADJUSTED, saved);
        auditService.record("FinancialTransaction", String.valueOf(id),
                "ADJUST", "adjustmentId=" + saved.getId() + " amount=" + request.adjustmentAmount(), null);
        return FinancialTransactionDto.from(saved);
    }

    // ── Financial calculations ────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public RetailerFinanceSummaryDto getRetailerSummary(Long retailerId) {
        Retailer retailer = retailerRepository.findById(retailerId)
                .orElseThrow(() -> new ResourceNotFoundException("Retailer", retailerId));

        BigDecimal totalBoxSales = txRepository.sumBoxSalesByRetailer(retailerId);
        BigDecimal totalReceived = txRepository.sumPaymentsReceivedByRetailer(retailerId);
        BigDecimal totalRecharge = txRepository.sumRechargeByRetailer(retailerId);
        BigDecimal outstanding = totalBoxSales.subtract(totalReceived);

        return new RetailerFinanceSummaryDto(
                retailer.getId(), retailer.getRetailerCode(), retailer.getRetailerName(),
                totalBoxSales, totalBoxSales, totalReceived, outstanding, totalRecharge);
    }

    @Transactional(readOnly = true)
    public FinanceSummaryDto getTenantSummary() {
        BigDecimal totalDue = txRepository.sumBoxSales();
        BigDecimal totalReceived = txRepository.sumPaymentsReceived();
        BigDecimal totalRecharge = txRepository.sumRecharge();
        long count = txRepository.countPosted();
        return new FinanceSummaryDto(totalDue, totalReceived, totalDue.subtract(totalReceived), totalRecharge, count);
    }

    // ── Package-private: used by FinanceUploadService ─────────────────────────

    @Transactional
    public FinancialTransaction createFromUpload(Retailer retailer,
                                                  TransactionType type, LocalDate date,
                                                  BigDecimal amount, PaymentMethod paymentMethod,
                                                  String reference, String paymentReference,
                                                  String description, String remarks) {
        if (txRepository.existsByReference(reference)) {
            throw new BusinessException("DUPLICATE_REFERENCE", "Duplicate reference: " + reference);
        }
        FinancialTransaction tx = buildTransaction(retailer, type, date, amount,
                paymentMethod, reference, paymentReference, description, remarks,
                TransactionSource.UPLOAD, null);
        return txRepository.save(tx);
    }

    // ── Notification helpers ───────────────────────────────────────────────────

    private void publishFinanceEvent(NotificationEventType eventType, FinancialTransaction tx) {
        try {
            BigDecimal totalDue = txRepository.sumBoxSalesByRetailer(tx.getRetailer().getId());
            BigDecimal totalReceived = txRepository.sumPaymentsReceivedByRetailer(tx.getRetailer().getId());
            BigDecimal totalRecharge = txRepository.sumRechargeByRetailer(tx.getRetailer().getId());
            java.util.Map<String, Object> payload = new java.util.LinkedHashMap<>();
            payload.put("transactionId", tx.getId());
            payload.put("transactionType", tx.getTransactionType().name());
            payload.put("transactionDate", tx.getTransactionDate().toString());
            payload.put("amount", tx.getAmount().toPlainString());
            payload.put("reference", tx.getReference());
            payload.put("retailerId", tx.getRetailer().getId());
            payload.put("retailerCode", tx.getRetailer().getRetailerCode());
            payload.put("retailerName", tx.getRetailer().getRetailerName());
            payload.put("totalDue", totalDue.toPlainString());
            payload.put("totalReceived", totalReceived.toPlainString());
            payload.put("outstanding", totalDue.subtract(totalReceived).toPlainString());
            payload.put("totalRecharge", totalRecharge.toPlainString());
            eventPublisher.publish(eventType, String.valueOf(tx.getId()), payload);
        } catch (Exception e) {
            log.warn("Failed to publish notification event for tx={}: {}", tx.getId(), e.getMessage());
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private String currentUsername() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "system";
    }

    private String resolveReference(String requested) {
        if (requested != null && !requested.isBlank()) {
            if (txRepository.existsByReference(requested.trim())) {
                throw new BusinessException("DUPLICATE_REFERENCE",
                        "Reference '" + requested + "' already exists");
            }
            return requested.trim();
        }
        return "MAN-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();
    }

    private FinancialTransaction buildTransaction(Retailer retailer,
                                                   TransactionType type, LocalDate date,
                                                   BigDecimal amount, PaymentMethod paymentMethod,
                                                   String reference, String paymentReference,
                                                   String description, String remarks,
                                                   TransactionSource source, StbSale sale) {
        FinancialTransaction tx = new FinancialTransaction();
        tx.setRetailer(retailer);
        tx.setTransactionType(type);
        tx.setTransactionStatus(TransactionStatus.POSTED);
        tx.setTransactionDate(date);
        tx.setAmount(amount);
        tx.setPaymentMethod(paymentMethod);
        tx.setReference(reference);
        tx.setPaymentReference(paymentReference);
        tx.setDescription(description);
        tx.setRemarks(remarks);
        tx.setSource(source);
        tx.setSale(sale);
        tx.setUpdatedBy(currentUsername());
        return tx;
    }
}
