package org.nexus.d2h.finance;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nexus.d2h.boxsale.StbSale;
import org.nexus.d2h.common.BusinessException;
import org.nexus.d2h.common.ResourceNotFoundException;
import org.nexus.d2h.retailer.Retailer;
import org.nexus.d2h.retailer.RetailerRepository;
import org.nexus.d2h.tenant.Tenant;
import org.nexus.d2h.tenant.TenantContext;
import org.nexus.d2h.tenant.TenantRepository;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FinanceServiceTest {

    @Mock FinancialTransactionRepository txRepository;
    @Mock TenantRepository tenantRepository;
    @Mock RetailerRepository retailerRepository;
    @InjectMocks FinanceService financeService;

    private Tenant tenant;
    private Retailer retailer;

    @BeforeEach
    void setUp() {
        tenant = new Tenant();
        tenant.setTenantCode("T1");
        setId(tenant, 1L);

        retailer = new Retailer();
        retailer.setTenant(tenant);
        retailer.setRetailerCode("RET001");
        retailer.setRetailerName("Test Retailer");
        retailer.setMobile("9876543210");
        setId(retailer, 5L);

        TenantContext.setCurrentTenant("T1");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("user", null, List.of()));
    }

    @BeforeEach
    void tearDown() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    void create_success() {
        when(tenantRepository.findByTenantCode("T1")).thenReturn(Optional.of(tenant));
        when(retailerRepository.findByIdAndTenantId(5L, 1L)).thenReturn(Optional.of(retailer));
        when(txRepository.existsByTenantIdAndReference(1L, "REF001")).thenReturn(false);
        when(txRepository.save(any())).thenAnswer(inv -> { setId(inv.getArgument(0), 100L); return inv.getArgument(0); });

        FinancialTransactionDto dto = financeService.create(new CreateTransactionRequest(
                5L, TransactionType.PAYMENT_RECEIVED, LocalDate.now(),
                BigDecimal.valueOf(5000), PaymentMethod.CASH, "REF001", null, null, null));

        assertThat(dto.transactionType()).isEqualTo(TransactionType.PAYMENT_RECEIVED);
        assertThat(dto.amount()).isEqualByComparingTo("5000");
        assertThat(dto.transactionStatus()).isEqualTo(TransactionStatus.POSTED);
    }

    @Test
    void create_duplicateReference_throwsBusinessException() {
        when(tenantRepository.findByTenantCode("T1")).thenReturn(Optional.of(tenant));
        when(retailerRepository.findByIdAndTenantId(5L, 1L)).thenReturn(Optional.of(retailer));
        when(txRepository.existsByTenantIdAndReference(1L, "REF001")).thenReturn(true);

        assertThatThrownBy(() -> financeService.create(new CreateTransactionRequest(
                5L, TransactionType.PAYMENT_RECEIVED, LocalDate.now(),
                BigDecimal.valueOf(5000), null, "REF001", null, null, null)))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", "DUPLICATE_REFERENCE");
    }

    @Test
    void create_autoGeneratesReferenceWhenBlank() {
        when(tenantRepository.findByTenantCode("T1")).thenReturn(Optional.of(tenant));
        when(retailerRepository.findByIdAndTenantId(5L, 1L)).thenReturn(Optional.of(retailer));
        when(txRepository.save(any())).thenAnswer(inv -> { setId(inv.getArgument(0), 100L); return inv.getArgument(0); });

        FinancialTransactionDto dto = financeService.create(new CreateTransactionRequest(
                5L, TransactionType.PAYMENT_RECEIVED, LocalDate.now(),
                BigDecimal.valueOf(5000), null, null, null, null, null));

        assertThat(dto.reference()).startsWith("MAN-");
    }

    @Test
    void reverse_postedTransaction_succeeds() {
        FinancialTransaction original = txWithId(10L, TransactionType.PAYMENT_RECEIVED, TransactionStatus.POSTED);
        when(tenantRepository.findByTenantCode("T1")).thenReturn(Optional.of(tenant));
        when(txRepository.findByIdAndTenantId(10L, 1L)).thenReturn(Optional.of(original));
        when(txRepository.save(any())).thenAnswer(inv -> { setId(inv.getArgument(0), 200L); return inv.getArgument(0); });

        FinancialTransactionDto dto = financeService.reverse(10L, "Incorrect payment");

        assertThat(dto.transactionType()).isEqualTo(TransactionType.REVERSAL);
        assertThat(dto.amount()).isNegative();
        assertThat(original.getTransactionStatus()).isEqualTo(TransactionStatus.REVERSED);
    }

    @Test
    void reverse_alreadyReversed_throwsBusinessException() {
        FinancialTransaction original = txWithId(10L, TransactionType.PAYMENT_RECEIVED, TransactionStatus.REVERSED);
        FinancialTransaction existingReversal = txWithId(11L, TransactionType.REVERSAL, TransactionStatus.POSTED);
        original.setReversedBy(existingReversal);
        when(tenantRepository.findByTenantCode("T1")).thenReturn(Optional.of(tenant));
        when(txRepository.findByIdAndTenantId(10L, 1L)).thenReturn(Optional.of(original));

        assertThatThrownBy(() -> financeService.reverse(10L, null))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", "ALREADY_REVERSED");
    }

    @Test
    void reverse_nonPostedTransaction_throwsBusinessException() {
        FinancialTransaction tx = txWithId(10L, TransactionType.PAYMENT_RECEIVED, TransactionStatus.PENDING);
        when(tenantRepository.findByTenantCode("T1")).thenReturn(Optional.of(tenant));
        when(txRepository.findByIdAndTenantId(10L, 1L)).thenReturn(Optional.of(tx));

        assertThatThrownBy(() -> financeService.reverse(10L, null))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", "TRANSACTION_NOT_REVERSIBLE");
    }

    @Test
    void adjust_postedTransaction_createsAdjustmentRecord() {
        FinancialTransaction original = txWithId(10L, TransactionType.PAYMENT_RECEIVED, TransactionStatus.POSTED);
        when(tenantRepository.findByTenantCode("T1")).thenReturn(Optional.of(tenant));
        when(txRepository.findByIdAndTenantId(10L, 1L)).thenReturn(Optional.of(original));
        when(txRepository.save(any())).thenAnswer(inv -> { setId(inv.getArgument(0), 300L); return inv.getArgument(0); });

        FinancialTransactionDto dto = financeService.adjust(10L,
                new AdjustTransactionRequest(BigDecimal.valueOf(-500), "Correction"));

        assertThat(dto.transactionType()).isEqualTo(TransactionType.ADJUSTMENT);
        assertThat(dto.amount()).isEqualByComparingTo("-500");
    }

    @Test
    void adjust_zeroAmount_throwsBusinessException() {
        FinancialTransaction original = txWithId(10L, TransactionType.PAYMENT_RECEIVED, TransactionStatus.POSTED);
        when(tenantRepository.findByTenantCode("T1")).thenReturn(Optional.of(tenant));
        when(txRepository.findByIdAndTenantId(10L, 1L)).thenReturn(Optional.of(original));

        assertThatThrownBy(() -> financeService.adjust(10L,
                new AdjustTransactionRequest(BigDecimal.ZERO, "reason")))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", "ZERO_ADJUSTMENT");
    }

    @Test
    void getRetailerSummary_calculatesCorrectly() {
        when(tenantRepository.findByTenantCode("T1")).thenReturn(Optional.of(tenant));
        when(retailerRepository.findByIdAndTenantId(5L, 1L)).thenReturn(Optional.of(retailer));
        when(txRepository.sumBoxSalesByRetailer(1L, 5L)).thenReturn(BigDecimal.valueOf(100000));
        when(txRepository.sumPaymentsReceivedByRetailer(1L, 5L)).thenReturn(BigDecimal.valueOf(70000));
        when(txRepository.sumRechargeByRetailer(1L, 5L)).thenReturn(BigDecimal.valueOf(15000));

        RetailerFinanceSummaryDto summary = financeService.getRetailerSummary(5L);

        assertThat(summary.totalDue()).isEqualByComparingTo("100000");
        assertThat(summary.totalReceived()).isEqualByComparingTo("70000");
        assertThat(summary.outstanding()).isEqualByComparingTo("30000");
        assertThat(summary.totalRecharge()).isEqualByComparingTo("15000");
    }

    @Test
    void getTenantSummary_calculatesOutstanding() {
        when(tenantRepository.findByTenantCode("T1")).thenReturn(Optional.of(tenant));
        when(txRepository.sumBoxSalesByTenant(1L)).thenReturn(BigDecimal.valueOf(200000));
        when(txRepository.sumPaymentsReceivedByTenant(1L)).thenReturn(BigDecimal.valueOf(150000));
        when(txRepository.sumRechargeByTenant(1L)).thenReturn(BigDecimal.valueOf(30000));
        when(txRepository.countPostedByTenant(1L)).thenReturn(50L);

        FinanceSummaryDto summary = financeService.getTenantSummary();

        assertThat(summary.outstanding()).isEqualByComparingTo("50000");
        assertThat(summary.transactionCount()).isEqualTo(50L);
    }

    @Test
    void recordBoxSale_idempotent_skipsIfAlreadyExists() {
        StbSale sale = saleWithId(20L);
        when(txRepository.existsByTenantIdAndSaleId(1L, 20L)).thenReturn(true);

        financeService.recordBoxSale(tenant, retailer, sale);

        verify(txRepository, never()).save(any());
    }

    @Test
    void tenantIsolation_cannotAccessOtherTenantTransaction() {
        when(tenantRepository.findByTenantCode("T1")).thenReturn(Optional.of(tenant));
        when(txRepository.findByIdAndTenantId(99L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> financeService.getById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void search_returnsPaginatedResults() {
        FinancialTransaction tx = txWithId(10L, TransactionType.PAYMENT_RECEIVED, TransactionStatus.POSTED);
        when(tenantRepository.findByTenantCode("T1")).thenReturn(Optional.of(tenant));
        when(txRepository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(tx)));

        var result = financeService.search(null, null, null, null, null, null, null, null, null,
                PageRequest.of(0, 20));

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void create_missingTenantContext_throwsBusinessException() {
        TenantContext.clear();

        assertThatThrownBy(() -> financeService.create(new CreateTransactionRequest(
                5L, TransactionType.PAYMENT_RECEIVED, LocalDate.now(),
                BigDecimal.valueOf(1000), null, null, null, null, null)))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", "TENANT_CONTEXT_MISSING");
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private FinancialTransaction txWithId(Long id, TransactionType type, TransactionStatus status) {
        FinancialTransaction tx = new FinancialTransaction();
        tx.setTenant(tenant);
        tx.setRetailer(retailer);
        tx.setTransactionType(type);
        tx.setTransactionStatus(status);
        tx.setTransactionDate(LocalDate.now());
        tx.setAmount(BigDecimal.valueOf(5000));
        tx.setSource(TransactionSource.MANUAL);
        setId(tx, id);
        return tx;
    }

    private StbSale saleWithId(Long id) {
        StbSale s = new StbSale();
        s.setTenant(tenant);
        s.setRetailer(retailer);
        s.setTransactionDate(LocalDate.now());
        s.setTotalAmount(BigDecimal.valueOf(3000));
        setId(s, id);
        return s;
    }

    private void setId(Object entity, Long id) {
        try {
            Class<?> cls = entity.getClass();
            java.lang.reflect.Field field = null;
            while (cls != null) {
                try { field = cls.getDeclaredField("id"); break; }
                catch (NoSuchFieldException e) { cls = cls.getSuperclass(); }
            }
            if (field == null) throw new NoSuchFieldException("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (Exception e) { throw new RuntimeException(e); }
    }
}
