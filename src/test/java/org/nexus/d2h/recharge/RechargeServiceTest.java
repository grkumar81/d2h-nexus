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
import org.nexus.d2h.notification.NotificationEventPublisher;
import org.nexus.d2h.retailer.Retailer;
import org.nexus.d2h.retailer.RetailerRepository;
import org.nexus.d2h.retailer.RetailerStatus;
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
    @Mock RetailerRepository retailerRepository;
    @Mock AssetRepository assetRepository;
    @Mock NotificationEventPublisher eventPublisher;
    @InjectMocks RechargeService rechargeService;

    private Retailer retailer;

    @BeforeEach
    void setUp() {
        retailer = new Retailer();
        retailer.setRetailerCode("RET001");
        retailer.setRetailerName("Test Retailer");
        retailer.setMobile("9876543210");
        retailer.setStatus(RetailerStatus.ACTIVE);
        setId(retailer, 5L);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("user", null, List.of()));
    }

    @BeforeEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void create_success_returnsDto() {
        when(retailerRepository.findById(5L)).thenReturn(Optional.of(retailer));
        when(rechargeRepository.existsByReference("RCH001")).thenReturn(false);
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
        when(retailerRepository.findById(5L)).thenReturn(Optional.of(retailer));
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
        when(retailerRepository.findById(5L)).thenReturn(Optional.of(retailer));
        when(rechargeRepository.existsByReference("DUP001")).thenReturn(true);

        assertThatThrownBy(() -> rechargeService.create(new CreateRechargeRequest(
                5L, null, LocalDate.now(), BigDecimal.valueOf(1000),
                RechargeType.REGULAR, null, null, null, null, null, null, "DUP001")))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", "DUPLICATE_REFERENCE");
    }

    @Test
    void create_inactiveRetailer_throwsBusinessException() {
        retailer.setStatus(RetailerStatus.INACTIVE);
        when(retailerRepository.findById(5L)).thenReturn(Optional.of(retailer));

        assertThatThrownBy(() -> rechargeService.create(new CreateRechargeRequest(
                5L, null, LocalDate.now(), BigDecimal.valueOf(1000),
                RechargeType.REGULAR, null, null, null, null, null, null, null)))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", "RETAILER_NOT_ACTIVE");
    }

    @Test
    void reverse_successRecharge_createsReversal() {
        RechargeTransaction original = rechargeWithId(10L, RechargeStatus.SUCCESS);
        when(rechargeRepository.findById(10L)).thenReturn(Optional.of(original));
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
        when(rechargeRepository.findById(10L)).thenReturn(Optional.of(original));

        assertThatThrownBy(() -> rechargeService.reverse(10L, null))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", "ALREADY_REVERSED");
    }

    @Test
    void reverse_nonSuccessRecharge_throwsBusinessException() {
        RechargeTransaction tx = rechargeWithId(10L, RechargeStatus.PENDING);
        when(rechargeRepository.findById(10L)).thenReturn(Optional.of(tx));

        assertThatThrownBy(() -> rechargeService.reverse(10L, null))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", "RECHARGE_NOT_REVERSIBLE");
    }

    @Test
    void cancel_pendingRecharge_succeeds() {
        RechargeTransaction tx = rechargeWithId(10L, RechargeStatus.PENDING);
        when(rechargeRepository.findById(10L)).thenReturn(Optional.of(tx));
        when(rechargeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RechargeTransactionDto dto = rechargeService.cancel(10L, "Cancelled by user");

        assertThat(dto.rechargeStatus()).isEqualTo(RechargeStatus.CANCELLED);
    }

    @Test
    void cancel_successRecharge_throwsBusinessException() {
        RechargeTransaction tx = rechargeWithId(10L, RechargeStatus.SUCCESS);
        when(rechargeRepository.findById(10L)).thenReturn(Optional.of(tx));

        assertThatThrownBy(() -> rechargeService.cancel(10L, "reason"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", "RECHARGE_NOT_CANCELLABLE");
    }

    @Test
    void getSummary_returnsAggregatedValues() {
        LocalDate from = LocalDate.of(2026, 1, 1);
        LocalDate to   = LocalDate.of(2026, 12, 31);
        when(rechargeRepository.sumSuccess(from, to)).thenReturn(BigDecimal.valueOf(48000));
        when(rechargeRepository.sumFailed(from, to)).thenReturn(BigDecimal.valueOf(2000));
        when(rechargeRepository.sumReversed(from, to)).thenReturn(BigDecimal.valueOf(1000));
        when(rechargeRepository.sumTotal(from, to)).thenReturn(BigDecimal.valueOf(51000));
        when(rechargeRepository.countAll(from, to)).thenReturn(20L);

        RechargeSummaryDto summary = rechargeService.getSummary(from, to);

        assertThat(summary.totalCount()).isEqualTo(20L);
        assertThat(summary.totalAmount()).isEqualByComparingTo("51000");
        assertThat(summary.successAmount()).isEqualByComparingTo("48000");
    }

    @Test
    void getRetailerSummary_returnsCorrectValues() {
        when(retailerRepository.findById(5L)).thenReturn(Optional.of(retailer));
        when(rechargeRepository.sumTotalByRetailer(5L)).thenReturn(BigDecimal.valueOf(50000));
        when(rechargeRepository.sumSuccessByRetailer(5L)).thenReturn(BigDecimal.valueOf(48000));
        when(rechargeRepository.countByRetailer(5L)).thenReturn(10L);
        when(rechargeRepository.lastRechargeDateByRetailer(5L)).thenReturn(LocalDate.of(2026, 8, 1));
        when(rechargeRepository.lastRechargeAmountByRetailer(5L)).thenReturn(BigDecimal.valueOf(5000));

        RetailerRechargeSummaryDto summary = rechargeService.getRetailerSummary(5L);

        assertThat(summary.totalRecharge()).isEqualByComparingTo("50000");
        assertThat(summary.successRecharge()).isEqualByComparingTo("48000");
        assertThat(summary.rechargeCount()).isEqualTo(10L);
    }

    @Test
    void getById_notFound_throwsResourceNotFoundException() {
        when(rechargeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> rechargeService.getById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void search_returnsPaginatedResults() {
        RechargeTransaction tx = rechargeWithId(10L, RechargeStatus.SUCCESS);
        when(rechargeRepository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(tx)));

        var result = rechargeService.search(null, null, null, null, null, null,
                null, null, null, PageRequest.of(0, 20));

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void createFromUpload_duplicateReference_throwsBusinessException() {
        when(rechargeRepository.existsByReference("REF001")).thenReturn(true);

        assertThatThrownBy(() -> rechargeService.createFromUpload(
                retailer, null, RechargeType.REGULAR, LocalDate.now(),
                BigDecimal.valueOf(1000), null, "REF001", null, null, null, null, null))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", "DUPLICATE_REFERENCE");
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private RechargeTransaction rechargeWithId(Long id, RechargeStatus status) {
        RechargeTransaction tx = new RechargeTransaction();
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
