package org.nexus.d2h.dashboard;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nexus.d2h.asset.AssetRepository;
import org.nexus.d2h.asset.AssetStatus;
import org.nexus.d2h.finance.FinancialTransactionRepository;
import org.nexus.d2h.recharge.RechargeTransactionRepository;
import org.nexus.d2h.retailer.RetailerRepository;
import org.nexus.d2h.retailer.RetailerStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock FinancialTransactionRepository financeRepo;
    @Mock RechargeTransactionRepository rechargeRepo;
    @Mock AssetRepository assetRepo;
    @Mock RetailerRepository retailerRepo;
    @InjectMocks DashboardService dashboardService;

    @Test
    void getDashboard_returnsAggregatedKpis() {
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
        assertThat(dto.totalRetailers()).isEqualTo(20L);
        assertThat(dto.activeRetailers()).isEqualTo(15L);
    }

    @Test
    void getDashboard_outstandingCalculatedCorrectly() {
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
    void getDashboard_zeroData_returnsZeroKpis() {
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

    private void stubFinanceAggregates(BigDecimal boxSales, BigDecimal received,
                                        BigDecimal recharge, long count) {
        when(financeRepo.sumBoxSalesByDateRange(any(), any())).thenReturn(boxSales);
        when(financeRepo.sumPaymentsReceivedByDateRange(any(), any())).thenReturn(received);
        when(financeRepo.sumRechargeByDateRange(any(), any())).thenReturn(recharge);
        when(financeRepo.countPostedByDateRange(any(), any())).thenReturn(count);
    }

    @SuppressWarnings("unchecked")
    private void stubAssets(long total, List<Object[]> statusCounts) {
        when(assetRepo.count()).thenReturn(total);
        when(assetRepo.countByStatus()).thenReturn((List) statusCounts);
    }

    private void stubRetailers(long total, long active, long inactive) {
        when(retailerRepo.count()).thenReturn(total);
        when(retailerRepo.countByStatus(RetailerStatus.ACTIVE)).thenReturn(active);
        when(retailerRepo.countByStatus(RetailerStatus.INACTIVE)).thenReturn(inactive);
    }

    @SuppressWarnings("unchecked")
    private void stubTopRetailers() {
        when(financeRepo.topRetailersByReceived(anyInt())).thenReturn((List) new ArrayList<Object[]>());
        when(financeRepo.topRetailersByOutstanding(anyInt())).thenReturn((List) new ArrayList<Object[]>());
    }

    @SuppressWarnings("unchecked")
    private void stubMonthlyTrend(List<Object[]> rows) {
        when(financeRepo.monthlyTrend(any(LocalDate.class), any(LocalDate.class))).thenReturn((List) rows);
    }

    private Object[] row(AssetStatus status, Long count) {
        return new Object[]{status, count};
    }
}
