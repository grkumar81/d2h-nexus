package org.nexus.d2h.recharge;

import lombok.extern.slf4j.Slf4j;
import org.nexus.d2h.asset.AssetRepository;
import org.nexus.d2h.asset.StbAsset;
import org.nexus.d2h.common.BusinessException;
import org.nexus.d2h.common.PageResponse;
import org.nexus.d2h.common.ResourceNotFoundException;
import org.nexus.d2h.finance.PaymentMethod;
import org.nexus.d2h.notification.NotificationEventPublisher;
import org.nexus.d2h.notification.NotificationEventType;
import org.nexus.d2h.retailer.Retailer;
import org.nexus.d2h.retailer.RetailerRepository;
import org.nexus.d2h.retailer.RetailerStatus;
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
public class RechargeService {

    private final RechargeTransactionRepository rechargeRepository;
    private final RetailerRepository retailerRepository;
    private final AssetRepository assetRepository;
    private final NotificationEventPublisher eventPublisher;

    public RechargeService(RechargeTransactionRepository rechargeRepository,
                           RetailerRepository retailerRepository,
                           AssetRepository assetRepository,
                           NotificationEventPublisher eventPublisher) {
        this.rechargeRepository = rechargeRepository;
        this.retailerRepository = retailerRepository;
        this.assetRepository = assetRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public RechargeTransactionDto create(CreateRechargeRequest request) {
        Retailer retailer = retailerRepository.findById(request.retailerId())
                .orElseThrow(() -> new ResourceNotFoundException("Retailer", request.retailerId()));
        validateRetailerActive(retailer);

        StbAsset asset = null;
        if (request.assetId() != null) {
            asset = assetRepository.findById(request.assetId())
                    .orElseThrow(() -> new ResourceNotFoundException("StbAsset", request.assetId()));
        }

        String ref = resolveReference(request.reference());

        RechargeTransaction tx = new RechargeTransaction();
        tx.setRetailer(retailer);
        tx.setAsset(asset);
        tx.setReference(ref);
        tx.setExternalReference(request.externalReference());
        tx.setRechargeDate(request.rechargeDate());
        tx.setAmount(request.amount());
        tx.setRechargeType(request.rechargeType());
        tx.setRechargeStatus(RechargeStatus.SUCCESS);
        tx.setPaymentMethod(request.paymentMethod());
        tx.setPaymentReference(request.paymentReference());
        tx.setServicePeriod(request.servicePeriod());
        tx.setDescription(request.description());
        tx.setRemarks(request.remarks());
        tx.setSource(RechargeSource.MANUAL);
        tx.setUpdatedBy(currentUsername());

        RechargeTransaction saved = rechargeRepository.save(tx);
        log.info("Recharge created: id={} ref={} amount={} retailer={}",
                saved.getId(), saved.getReference(), saved.getAmount(), retailer.getRetailerCode());
        publishRechargeEvent(NotificationEventType.RECHARGE_CREATED, saved);
        return RechargeTransactionDto.from(saved);
    }

    @Transactional(readOnly = true)
    public RechargeTransactionDto getById(Long id) {
        return RechargeTransactionDto.from(rechargeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("RechargeTransaction", id)));
    }

    @Transactional(readOnly = true)
    public PageResponse<RechargeTransactionDto> search(
            Long retailerId, RechargeType type, RechargeStatus status,
            PaymentMethod paymentMethod, LocalDate dateFrom, LocalDate dateTo,
            String reference, BigDecimal amountMin, BigDecimal amountMax,
            Pageable pageable) {

        Page<RechargeTransactionDto> page = rechargeRepository.findAll(
                RechargeSpecification.search(retailerId, type, status,
                        paymentMethod, dateFrom, dateTo, reference, amountMin, amountMax),
                pageable).map(RechargeTransactionDto::from);
        return PageResponse.from(page);
    }

    @Transactional
    public RechargeTransactionDto reverse(Long id, String reason) {
        RechargeTransaction original = rechargeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("RechargeTransaction", id));

        if (original.getReversedBy() != null) {
            throw new BusinessException("ALREADY_REVERSED", "Recharge has already been reversed");
        }
        if (original.getRechargeStatus() != RechargeStatus.SUCCESS) {
            throw new BusinessException("RECHARGE_NOT_REVERSIBLE", "Only SUCCESS recharges can be reversed");
        }

        String reversalRef = "REV-" + original.getId() + "-"
                + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        RechargeTransaction reversal = new RechargeTransaction();
        reversal.setRetailer(original.getRetailer());
        reversal.setAsset(original.getAsset());
        reversal.setReference(reversalRef);
        reversal.setRechargeDate(LocalDate.now());
        reversal.setAmount(original.getAmount().negate());
        reversal.setRechargeType(original.getRechargeType());
        reversal.setRechargeStatus(RechargeStatus.REVERSED);
        reversal.setPaymentMethod(original.getPaymentMethod());
        reversal.setDescription("Reversal of recharge #" + original.getId()
                + (reason != null ? ": " + reason : ""));
        reversal.setRemarks(reason);
        reversal.setSource(RechargeSource.MANUAL);
        reversal.setReversalOf(original);
        reversal.setUpdatedBy(currentUsername());

        RechargeTransaction savedReversal = rechargeRepository.save(reversal);

        original.setRechargeStatus(RechargeStatus.REVERSED);
        original.setReversedBy(savedReversal);
        original.setUpdatedBy(currentUsername());
        rechargeRepository.save(original);

        log.info("Recharge {} reversed by {} — reversal id={}", id, currentUsername(), savedReversal.getId());
        publishRechargeEvent(NotificationEventType.RECHARGE_REVERSED, savedReversal);
        return RechargeTransactionDto.from(savedReversal);
    }

    @Transactional
    public RechargeTransactionDto cancel(Long id, String reason) {
        RechargeTransaction tx = rechargeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("RechargeTransaction", id));

        if (tx.getRechargeStatus() != RechargeStatus.PENDING
                && tx.getRechargeStatus() != RechargeStatus.FAILED) {
            throw new BusinessException("RECHARGE_NOT_CANCELLABLE",
                    "Only PENDING or FAILED recharges can be cancelled");
        }

        tx.setRechargeStatus(RechargeStatus.CANCELLED);
        tx.setRemarks(reason);
        tx.setUpdatedBy(currentUsername());
        return RechargeTransactionDto.from(rechargeRepository.save(tx));
    }

    @Transactional(readOnly = true)
    public RechargeSummaryDto getSummary(LocalDate dateFrom, LocalDate dateTo) {
        BigDecimal success  = rechargeRepository.sumSuccess(dateFrom, dateTo);
        BigDecimal failed   = rechargeRepository.sumFailed(dateFrom, dateTo);
        BigDecimal reversed = rechargeRepository.sumReversed(dateFrom, dateTo);
        BigDecimal total    = rechargeRepository.sumTotal(dateFrom, dateTo);
        long count          = rechargeRepository.countAll(dateFrom, dateTo);
        return new RechargeSummaryDto(count, total, success, failed, reversed, dateFrom, dateTo);
    }

    @Transactional(readOnly = true)
    public RetailerRechargeSummaryDto getRetailerSummary(Long retailerId) {
        Retailer retailer = retailerRepository.findById(retailerId)
                .orElseThrow(() -> new ResourceNotFoundException("Retailer", retailerId));

        BigDecimal total   = rechargeRepository.sumTotalByRetailer(retailerId);
        BigDecimal success = rechargeRepository.sumSuccessByRetailer(retailerId);
        long count         = rechargeRepository.countByRetailer(retailerId);
        LocalDate lastDate = rechargeRepository.lastRechargeDateByRetailer(retailerId);
        BigDecimal lastAmt = rechargeRepository.lastRechargeAmountByRetailer(retailerId);

        return new RetailerRechargeSummaryDto(
                retailer.getId(), retailer.getRetailerCode(), retailer.getRetailerName(),
                count, total, success, lastDate, lastAmt);
    }

    @Transactional
    public RechargeTransaction createFromUpload(Retailer retailer, StbAsset asset,
                                                 RechargeType type, LocalDate date, BigDecimal amount,
                                                 PaymentMethod paymentMethod, String reference,
                                                 String externalReference, String paymentReference,
                                                 String servicePeriod, String description, String remarks) {
        if (rechargeRepository.existsByReference(reference)) {
            throw new BusinessException("DUPLICATE_REFERENCE", "Duplicate reference: " + reference);
        }
        RechargeTransaction tx = new RechargeTransaction();
        tx.setRetailer(retailer);
        tx.setAsset(asset);
        tx.setReference(reference);
        tx.setExternalReference(externalReference);
        tx.setRechargeDate(date);
        tx.setAmount(amount);
        tx.setRechargeType(type);
        tx.setRechargeStatus(RechargeStatus.SUCCESS);
        tx.setPaymentMethod(paymentMethod);
        tx.setPaymentReference(paymentReference);
        tx.setServicePeriod(servicePeriod);
        tx.setDescription(description);
        tx.setRemarks(remarks);
        tx.setSource(RechargeSource.UPLOAD);
        tx.setUpdatedBy("upload");
        return rechargeRepository.save(tx);
    }

    private void publishRechargeEvent(NotificationEventType eventType, RechargeTransaction tx) {
        try {
            java.util.Map<String, Object> payload = new java.util.LinkedHashMap<>();
            payload.put("rechargeId", tx.getId());
            payload.put("rechargeType", tx.getRechargeType().name());
            payload.put("rechargeDate", tx.getRechargeDate().toString());
            payload.put("amount", tx.getAmount().toPlainString());
            payload.put("reference", tx.getReference());
            payload.put("rechargeStatus", tx.getRechargeStatus().name());
            payload.put("retailerId", tx.getRetailer().getId());
            payload.put("retailerCode", tx.getRetailer().getRetailerCode());
            payload.put("retailerName", tx.getRetailer().getRetailerName());
            eventPublisher.publish(eventType, String.valueOf(tx.getId()), payload);
        } catch (Exception e) {
            log.warn("Failed to publish recharge notification event for tx={}: {}", tx.getId(), e.getMessage());
        }
    }

    private String resolveReference(String requested) {
        if (requested != null && !requested.isBlank()) {
            if (rechargeRepository.existsByReference(requested.trim())) {
                throw new BusinessException("DUPLICATE_REFERENCE",
                        "Reference '" + requested + "' already exists");
            }
            return requested.trim();
        }
        return "RCH-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();
    }

    private void validateRetailerActive(Retailer retailer) {
        if (retailer.getStatus() != RetailerStatus.ACTIVE) {
            throw new BusinessException("RETAILER_NOT_ACTIVE",
                    "Retailer '" + retailer.getRetailerCode() + "' is not active");
        }
    }

    private String currentUsername() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "system";
    }
}
