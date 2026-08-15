package org.nexus.d2h.recharge;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nexus.d2h.asset.AssetRepository;
import org.nexus.d2h.common.BusinessException;
import org.nexus.d2h.common.ResourceNotFoundException;
import org.nexus.d2h.retailer.Retailer;
import org.nexus.d2h.retailer.RetailerRepository;
import org.nexus.d2h.retailer.RetailerStatus;
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
class RechargeServiceTest {

    @Mock RechargeTransactionRepository rechargeRepository;
    @Mock TenantRepository tenantRepository;
    @Mock RetailerRepository retailerRepository;
    @Mock AssetRepository assetRepository;
    @InjectMocks RechargeService rechargeService;

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
        retailer.setStatus(RetailerStatus.ACTIVE);
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

    // ── Create ────────────────────────────────────────────────────────────────

    @Test
    void create_success_returnsDto() {
        when(tenantRepository.findByTenantCode("T1")).thenReturn(Optional.of(tenant));
        when(retailerRepository.findByIdAndTenantId(5L, 1L)).thenReturn(Optional.of(retailer));
        when(rechargeRepository.existsByTenantIdAndReference(1L, "RCH001")).thenReturn(false);
        when(rechargeRepository.save(any())).thenAnswer(inv -> {
            setId(inv.getArgument(0), 100L);
            return inv.getArgument(0);
        });

        RechargeTransactionDto dto = rechargeService.create(new CreateRechargeRequest(
                5L, null, LocalDate.now(), BigDecimal.valueOf(1000),
                RechargeType.REGULAR, null, null, null, null, null, null, "RCH001"));

        assertThat(dto.amount()).isEqualByComparingTo("1000");
        assertThat(dto.rechargeStatus()).isEqualTo(RechargeStatus.SUCCESS);
        assertThat(dto.rechargeType()).isEqualTo(RechargeType.REGULAR);
        assertThat(dto.reference()).isEqualTo("RCH001");
    }

    @Test
    void create_autoGeneratesReference_whenBlank() {
        when(tenantRepository.findByTenantCode("T1")).thenReturn(Optional.of(tenant));
        when(retailerRepository.findByIdAndTenantId(5L, 1L)).thenReturn(Optional.of(retailer));
        when(rechargeRepository.save(any())).thenAnswer(inv -> {
            setId(inv.getArgument(0), 100L);
            return inv.getArgument(0);
        });

        RechargeTransactionDto dto = rechargeService.create(new CreateRechargeRequest(
                5L, null, LocalDate.now(), BigDecimal.valueOf(500),
                RechargeType.MONTHLY, null, null, null, null, null, null, null));

        assertThat(dto.reference()).startsWith("RCH-");
    }

    @Test
    void create_duplicateReference_throwsBusinessException() {
        when(tenantRepository.findByTenantCode("T1")).thenReturn(Optional.of(tenant));
        when(retailerRepository.findByIdAndTenantId(5L, 1L)).thenReturn(Optional.of(retailer));
        when(rechargeRepository.existsByTenantIdAndReference(1L, "DUP001")).thenReturn(true);

        assertThatThrownBy(() -> rechargeService.create(new CreateRechargeRequest(
                5L, null, LocalDate.now(), BigDecimal.valueOf(1000),
                RechargeType.REGULAR, null, null, null, null, null, null, "DUP001")))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", "DUPLICATE_REFERENCE");
    }

    @Test
    void create_inactiveRetailer_throwsBusinessException() {
        retailer.setStatus(RetailerStatus.INACTIVE);
        when(tenantRepository.findByTenantCode("T1")).thenReturn(Optional.of(tenant));
        when(retailerRepository.findByIdAndTenantId(5L, 1L)).thenReturn(Optional.of(retailer));

        assertThatThrownBy(() -> rechargeService.create(new CreateRechargeRequest(
                5L, null, LocalDate.now(), BigDecimal.valueOf(1000),
                RechargeType.REGULAR, null, null, null, null, null, null, null)))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", "RETAILER_NOT_ACTIVE");
    }

    @Test
    void create_missingTenantContext_throwsBusinessException() {
        TenantContext.clear();

        assertThatThrownBy(() -> rechargeService.create(new CreateRechargeRequest(
                5L, null, LocalDate.now(), BigDecimal.valueOf(1000),
                RechargeType.REGULAR, null, null, null, null, null, null, null)))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", "TENANT_CONTEXT_MISSING");
    }

    // ── Reversal ──────────────────────────────────────────────────────────────

    @Test
    void reverse_successRecharge_createsReversal() {
        RechargeTransaction original = rechargeWithId(10L, RechargeStatus.SUCCESS);
        when(tenantRepository.findByTenantCode("T1")).thenReturn(Optional.of(tenant));
        when(rechargeRepository.findByIdAndTenantId(10L, 1L)).thenReturn(Optional.of(original));
        when(rechargeRepository.save(any())).thenAnswer(inv -> {
            setId(inv.getArgument(0), 200L);
            return inv.getArgument(0);
        });

        RechargeTransactionDto dto = rechargeService.reverse(10L, "Wrong retailer");

        assertThat(dto.rechargeStatus()).isEqualTo(RechargeStatus.REVERSED);
        assertThat(dto.amount()).isNegative();
        assertThat(original.getRechargeStatus()).isEqualTo(RechargeStatus.REVERSED);
        assertThat(original.getReversedBy()).isNotNull();
    }

    @Test
    void reverse_alreadyReversed_throwsBusinessException() {
        RechargeTransaction original = rechargeWithId(10L, RechargeStatus.REVERSED);
        original.setReversedBy(rechargeWithId(11L, RechargeStatus.REVERSED));
        when(tenantRepository.findByTenantCode("T1")).thenReturn(Optional.of(tenant));
        when(rechargeRepository.findByIdAndTenantId(10L, 1L)).thenReturn(Optional.of(original));

        assertThatThrownBy(() -> rechargeService.reverse(10L, null))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", "ALREADY_REVERSED");
    }

    @Test
    void reverse_nonSuccessRecharge_throwsBusinessException() {
        RechargeTransaction tx = rechargeWithId(10L, RechargeStatus.PENDING);
        when(tenantRepository.findByTenantCode("T1")).thenReturn(Optional.of(tenant));
        when(rechargeRepository.findByIdAndTenantId(10L, 1L)).thenReturn(Optional.of(tx));

        assertThatThrownBy(() -> rechargeService.reverse(10L, null))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", "RECHARGE_NOT_REVERSIBLE");
    }

    // ── Cancellation ──────────────────────────────────────────────────────────

    @Test
    void cancel_pendingRecharge_succeeds() {
        RechargeTransaction tx = rechargeWithId(10L, RechargeStatus.PENDING);
        when(tenantRepository.findByTenantCode("T1")).thenReturn(Optional.of(tenant));
        when(rechargeRepository.findByIdAndTenantId(10L, 1L)).thenReturn(Optional.of(tx));
        when(rechargeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RechargeTransactionDto dto = rechargeService.cancel(10L, "Cancelled by user");

        assertThat(dto.rechargeStatus()).isEqualTo(RechargeStatus.CANCELLED);
    }

    @Test
    void cancel_successRecharge_throwsBusinessException() {
        RechargeTransaction tx = rechargeWithId(10L, RechargeStatus.SUCCESS);
        when(tenantRepository.findByTenantCode("T1")).thenReturn(Optional.of(tenant));
        when(rechargeRepository.findByIdAndTenantId(10L, 1L)).thenReturn(Optional.of(tx));

        assertThatThrownBy(() -> rechargeService.cancel(10L, "reason"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", "RECHARGE_NOT_CANCELLABLE");
    }

    // ── Summary ───────────────────────────────────────────────────────────────

    @Test
    void getSummary_returnsAggregatedValues() {
        LocalDate from = LocalDate.of(2026, 1, 1);
        LocalDate to   = LocalDate.of(2026, 12, 31);
        when(tenantRepository.findByTenantCode("T1")).thenReturn(Optional.of(tenant));
        when(rechargeRepository.sumSuccessByTenant(1L, from, to)).thenReturn(BigDecimal.valueOf(48000));
        when(rechargeRepository.sumFailedByTenant(1L, from, to)).thenReturn(BigDecimal.valueOf(2000));
        when(rechargeRepository.sumReversedByTenant(1L, from, to)).thenReturn(BigDecimal.valueOf(1000));
        when(rechargeRepository.sumTotalByTenant(1L, from, to)).thenReturn(BigDecimal.valueOf(51000));
        when(rechargeRepository.countByTenant(1L, from, to)).thenReturn(20L);

        RechargeSummaryDto summary = rechargeService.getSummary(from, to);

        assertThat(summary.totalCount()).isEqualTo(20L);
        assertThat(summary.totalAmount()).isEqualByComparingTo("51000");
        assertThat(summary.successAmount()).isEqualByComparingTo("48000");
        assertThat(summary.failedAmount()).isEqualByComparingTo("2000");
        assertThat(summary.reversedAmount()).isEqualByComparingTo("1000");
    }

    @Test
    void getRetailerSummary_returnsCorrectValues() {
        when(tenantRepository.findByTenantCode("T1")).thenReturn(Optional.of(tenant));
        when(retailerRepository.findByIdAndTenantId(5L, 1L)).thenReturn(Optional.of(retailer));
        when(rechargeRepository.sumTotalByRetailer(1L, 5L)).thenReturn(BigDecimal.valueOf(50000));
        when(rechargeRepository.sumSuccessByRetailer(1L, 5L)).thenReturn(BigDecimal.valueOf(48000));
        when(rechargeRepository.countByRetailer(1L, 5L)).thenReturn(10L);
        when(rechargeRepository.lastRechargeDateByRetailer(1L, 5L)).thenReturn(LocalDate.of(2026, 8, 1));
        when(rechargeRepository.lastRechargeAmountByRetailer(1L, 5L)).thenReturn(BigDecimal.valueOf(5000));

        RetailerRechargeSummaryDto summary = rechargeService.getRetailerSummary(5L);

        assertThat(summary.totalRecharge()).isEqualByComparingTo("50000");
        assertThat(summary.successRecharge()).isEqualByComparingTo("48000");
        assertThat(summary.rechargeCount()).isEqualTo(10L);
        assertThat(summary.lastRechargeAmount()).isEqualByComparingTo("5000");
    }

    // ── Tenant isolation ──────────────────────────────────────────────────────

    @Test
    void tenantIsolation_cannotAccessOtherTenantRecharge() {
        when(tenantRepository.findByTenantCode("T1")).thenReturn(Optional.of(tenant));
        when(rechargeRepository.findByIdAndTenantId(99L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> rechargeService.getById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void search_returnsPaginatedResults() {
        RechargeTransaction tx = rechargeWithId(10L, RechargeStatus.SUCCESS);
        when(tenantRepository.findByTenantCode("T1")).thenReturn(Optional.of(tenant));
        when(rechargeRepository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(tx)));

        var result = rechargeService.search(null, null, null, null, null, null,
                null, null, null, PageRequest.of(0, 20));

        assertThat(result.getContent()).hasSize(1);
    }

    // ── createFromUpload ──────────────────────────────────────────────────────

    @Test
    void createFromUpload_duplicateReference_throwsBusinessException() {
        when(rechargeRepository.existsByTenantIdAndReference(1L, "REF001")).thenReturn(true);

        assertThatThrownBy(() -> rechargeService.createFromUpload(
                tenant, retailer, null, RechargeType.REGULAR, LocalDate.now(),
                BigDecimal.valueOf(1000), null, "REF001", null, null, null, null, null))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", "DUPLICATE_REFERENCE");
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private RechargeTransaction rechargeWithId(Long id, RechargeStatus status) {
        RechargeTransaction tx = new RechargeTransaction();
        tx.setTenant(tenant);
        tx.setRetailer(retailer);
        tx.setReference("RCH-" + id);
        tx.setRechargeDate(LocalDate.now());
        tx.setAmount(BigDecimal.valueOf(1000));
        tx.setRechargeType(RechargeType.REGULAR);
        tx.setRechargeStatus(status);
        tx.setSource(RechargeSource.MANUAL);
        setId(tx, id);
        return tx;
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
