package org.nexus.d2h.dashboard;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nexus.d2h.asset.AssetRepository;
import org.nexus.d2h.asset.AssetStatus;
import org.nexus.d2h.common.BusinessException;
import org.nexus.d2h.finance.FinancialTransactionRepository;
import org.nexus.d2h.recharge.RechargeTransactionRepository;
import org.nexus.d2h.retailer.RetailerRepository;
import org.nexus.d2h.retailer.RetailerStatus;
import org.nexus.d2h.tenant.Tenant;
import org.nexus.d2h.tenant.TenantContext;
import org.nexus.d2h.tenant.TenantRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock FinancialTransactionRepository financeRepo;
    @Mock RechargeTransactionRepository rechargeRepo;
    @Mock AssetRepository assetRepo;
    @Mock RetailerRepository retailerRepo;
    @Mock TenantRepository tenantRepository;
    @InjectMocks DashboardService dashboardService;

    private Tenant tenant;

    @BeforeEach
    void setUp() {
        tenant = new Tenant();
        tenant.setTenantCode("T1");
        setId(tenant, 1L);
        TenantContext.setCurrentTenant("T1");
    }

    @BeforeEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void getDashboard_returnsAggregatedKpis() {
        stubTenant();
        stubFinanceAggregates(BigDecimal.valueOf(100000), BigDecimal.valueOf(70000), BigDecimal.valueOf(20000), 50L);
        stubAssets(10L, List.of(
                row(AssetStatus.AVAILABLE, 4L),
                row(AssetStatus.ALLOCATED, 3L),
                row(AssetStatus.SOLD, 2L),
                row(AssetStatus.ACTIVATED, 1L)
        ));
        stubRetailers(20L, 15L, 5L);
        stubTopRetailers();
        stubMonthlyTrend(List.of());

        DashboardDto dto = dashboardService.getDashboard(null);

        assertThat(dto.totalBoxSales()).isEqualByComparingTo("100000");
        assertThat(dto.totalReceived()).isEqualByComparingTo("70000");
        assertThat(dto.totalOutstanding()).isEqualByComparingTo("30000");
        assertThat(dto.totalRecharge()).isEqualByComparingTo("20000");
        assertThat(dto.transactionCount()).isEqualTo(50L);
        assertThat(dto.totalAssets()).isEqualTo(10L);
        assertThat(dto.availableAssets()).isEqualTo(4L);
        assertThat(dto.allocatedAssets()).isEqualTo(3L);
        assertThat(dto.soldAssets()).isEqualTo(2L);
        assertThat(dto.activatedAssets()).isEqualTo(1L);
        assertThat(dto.totalRetailers()).isEqualTo(20L);
        assertThat(dto.activeRetailers()).isEqualTo(15L);
        assertThat(dto.inactiveRetailers()).isEqualTo(5L);
    }

    @Test
    void getDashboard_outstandingCalculatedCorrectly() {
        stubTenant();
        stubFinanceAggregates(BigDecimal.valueOf(50000), BigDecimal.valueOf(30000), BigDecimal.ZERO, 10L);
        stubAssets(0L, List.of());
        stubRetailers(5L, 5L, 0L);
        stubTopRetailers();
        stubMonthlyTrend(List.of());

        DashboardDto dto = dashboardService.getDashboard(null);

        assertThat(dto.totalOutstanding()).isEqualByComparingTo("20000");
    }

    @Test
    void getDashboard_withExplicitFyYear_usesCorrectDateRange() {
        stubTenant();
        stubFinanceAggregates(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 0L);
        stubAssets(0L, List.of());
        stubRetailers(0L, 0L, 0L);
        stubTopRetailers();
        stubMonthlyTrend(List.of());

        DashboardDto dto = dashboardService.getDashboard(2023);

        assertThat(dto.financialYearStart()).isEqualTo(2023);
        assertThat(dto.financialYearEnd()).isEqualTo(2024);
    }

    @Test
    void getDashboard_monthlyTrendMapped() {
        stubTenant();
        stubFinanceAggregates(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 0L);
        stubAssets(0L, List.of());
        stubRetailers(0L, 0L, 0L);
        stubTopRetailers();
        List<Object[]> trend = new ArrayList<>();
        trend.add(new Object[]{2024, 4, BigDecimal.valueOf(10000), BigDecimal.valueOf(8000), BigDecimal.valueOf(2000)});
        stubMonthlyTrend(trend);

        DashboardDto dto = dashboardService.getDashboard(2024);

        assertThat(dto.monthlyTrend()).hasSize(1);
        MonthlyTrendDto month = dto.monthlyTrend().get(0);
        assertThat(month.year()).isEqualTo(2024);
        assertThat(month.month()).isEqualTo(4);
        assertThat(month.boxSales()).isEqualByComparingTo("10000");
        assertThat(month.outstanding()).isEqualByComparingTo("2000");
    }

    @Test
    void getDashboard_topRetailersMapped() {
        stubTenant();
        stubFinanceAggregates(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 0L);
        stubAssets(0L, List.of());
        stubRetailers(0L, 0L, 0L);
        stubMonthlyTrend(List.of());
        List<Object[]> topReceived = new ArrayList<>();
        topReceived.add(new Object[]{10L, "RET001", "Retailer One", BigDecimal.valueOf(50000)});
        List<Object[]> topOutstanding = new ArrayList<>();
        topOutstanding.add(new Object[]{11L, "RET002", "Retailer Two", BigDecimal.valueOf(15000)});
        when(financeRepo.topRetailersByReceived(eq(1L), anyInt())).thenReturn((List) topReceived);
        when(financeRepo.topRetailersByOutstanding(eq(1L), anyInt())).thenReturn((List) topOutstanding);

        DashboardDto dto = dashboardService.getDashboard(null);

        assertThat(dto.topByReceived()).hasSize(1);
        assertThat(dto.topByReceived().get(0).retailerCode()).isEqualTo("RET001");
        assertThat(dto.topByOutstanding()).hasSize(1);
        assertThat(dto.topByOutstanding().get(0).amount()).isEqualByComparingTo("15000");
    }

    @Test
    void getDashboard_missingTenantContext_throwsBusinessException() {
        TenantContext.clear();

        assertThatThrownBy(() -> dashboardService.getDashboard(null))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", "TENANT_CONTEXT_MISSING");
    }

    @Test
    void getDashboard_zeroData_returnsZeroKpis() {
        stubTenant();
        stubFinanceAggregates(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 0L);
        stubAssets(0L, List.of());
        stubRetailers(0L, 0L, 0L);
        stubTopRetailers();
        stubMonthlyTrend(List.of());

        DashboardDto dto = dashboardService.getDashboard(null);

        assertThat(dto.totalBoxSales()).isEqualByComparingTo("0");
        assertThat(dto.totalOutstanding()).isEqualByComparingTo("0");
        assertThat(dto.totalAssets()).isEqualTo(0L);
        assertThat(dto.monthlyTrend()).isEmpty();
    }

    // ── stubs ─────────────────────────────────────────────────────────────────

    private void stubTenant() {
        when(tenantRepository.findByTenantCode("T1")).thenReturn(Optional.of(tenant));
    }

    private void stubFinanceAggregates(BigDecimal boxSales, BigDecimal received,
                                        BigDecimal recharge, long count) {
        when(financeRepo.sumBoxSalesByTenantAndDateRange(eq(1L), any(), any())).thenReturn(boxSales);
        when(financeRepo.sumPaymentsReceivedByTenantAndDateRange(eq(1L), any(), any())).thenReturn(received);
        when(financeRepo.sumRechargeByTenantAndDateRange(eq(1L), any(), any())).thenReturn(recharge);
        when(financeRepo.countPostedByTenantAndDateRange(eq(1L), any(), any())).thenReturn(count);
    }

    @SuppressWarnings("unchecked")
    private void stubAssets(long total, List<Object[]> statusCounts) {
        when(assetRepo.countByTenantId(1L)).thenReturn(total);
        when(assetRepo.countByStatusForTenant(1L)).thenReturn((List) statusCounts);
    }

    private void stubRetailers(long total, long active, long inactive) {
        when(retailerRepo.countByTenantId(1L)).thenReturn(total);
        when(retailerRepo.countByTenantIdAndStatus(1L, RetailerStatus.ACTIVE)).thenReturn(active);
        when(retailerRepo.countByTenantIdAndStatus(1L, RetailerStatus.INACTIVE)).thenReturn(inactive);
    }

    @SuppressWarnings("unchecked")
    private void stubTopRetailers() {
        when(financeRepo.topRetailersByReceived(eq(1L), anyInt())).thenReturn((List) new ArrayList<Object[]>());
        when(financeRepo.topRetailersByOutstanding(eq(1L), anyInt())).thenReturn((List) new ArrayList<Object[]>());
    }

    @SuppressWarnings("unchecked")
    private void stubMonthlyTrend(List<Object[]> rows) {
        when(financeRepo.monthlyTrend(eq(1L), any(LocalDate.class), any(LocalDate.class))).thenReturn((List) rows);
    }

    private Object[] row(AssetStatus status, Long count) {
        return new Object[]{status, count};
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
