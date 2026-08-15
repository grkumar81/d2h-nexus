package org.nexus.d2h.dashboard;

import lombok.extern.slf4j.Slf4j;
import org.nexus.d2h.asset.AssetRepository;
import org.nexus.d2h.asset.AssetStatus;
import org.nexus.d2h.finance.FinancialTransactionRepository;
import org.nexus.d2h.recharge.RechargeTransactionRepository;
import org.nexus.d2h.retailer.RetailerRepository;
import org.nexus.d2h.retailer.RetailerStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class DashboardService {

    private static final int TOP_RETAILERS_LIMIT = 5;

    private final FinancialTransactionRepository financeRepo;
    private final RechargeTransactionRepository rechargeRepo;
    private final AssetRepository assetRepo;
    private final RetailerRepository retailerRepo;

    public DashboardService(FinancialTransactionRepository financeRepo,
                            RechargeTransactionRepository rechargeRepo,
                            AssetRepository assetRepo,
                            RetailerRepository retailerRepo) {
        this.financeRepo = financeRepo;
        this.rechargeRepo = rechargeRepo;
        this.assetRepo = assetRepo;
        this.retailerRepo = retailerRepo;
    }

    @Transactional(readOnly = true)
    public DashboardDto getDashboard(Integer fyYear) {
        int startYear = (fyYear != null) ? fyYear : financialYearStartYear(LocalDate.now());
        LocalDate dateFrom = LocalDate.of(startYear, Month.APRIL, 1);
        LocalDate dateTo = LocalDate.of(startYear + 1, Month.MARCH, 31);

        BigDecimal boxSales = financeRepo.sumBoxSalesByDateRange(dateFrom, dateTo);
        BigDecimal received = financeRepo.sumPaymentsReceivedByDateRange(dateFrom, dateTo);
        BigDecimal recharge = financeRepo.sumRechargeByDateRange(dateFrom, dateTo);
        long txCount = financeRepo.countPostedByDateRange(dateFrom, dateTo);
        BigDecimal outstanding = boxSales.subtract(received);

        long totalAssets = assetRepo.count();
        Map<AssetStatus, Long> assetCounts = assetCountsByStatus();

        long totalRetailers = retailerRepo.count();
        long activeRetailers = retailerRepo.countByStatus(RetailerStatus.ACTIVE);
        long inactiveRetailers = retailerRepo.countByStatus(RetailerStatus.INACTIVE);
        long retailersWithOutstanding = countRetailersWithOutstanding();

        List<MonthlyTrendDto> trend = buildMonthlyTrend(dateFrom, dateTo);
        List<TopRetailerDto> topByReceived = buildTopByReceived();
        List<TopRetailerDto> topByOutstanding = buildTopByOutstanding();

        log.debug("Dashboard loaded for fy={}/{}", startYear, startYear + 1);

        return new DashboardDto(
                boxSales, received, outstanding, recharge, txCount,
                totalAssets,
                assetCounts.getOrDefault(AssetStatus.AVAILABLE, 0L),
                assetCounts.getOrDefault(AssetStatus.ALLOCATED, 0L),
                assetCounts.getOrDefault(AssetStatus.SOLD, 0L),
                assetCounts.getOrDefault(AssetStatus.ACTIVATED, 0L),
                assetCounts.getOrDefault(AssetStatus.RETURNED, 0L),
                assetCounts.getOrDefault(AssetStatus.DAMAGED, 0L),
                assetCounts.getOrDefault(AssetStatus.LOST, 0L),
                totalRetailers, activeRetailers, inactiveRetailers, retailersWithOutstanding,
                trend, topByReceived, topByOutstanding,
                startYear, startYear + 1
        );
    }

    private Map<AssetStatus, Long> assetCountsByStatus() {
        Map<AssetStatus, Long> map = new EnumMap<>(AssetStatus.class);
        for (Object[] row : assetRepo.countByStatus()) {
            map.put((AssetStatus) row[0], (Long) row[1]);
        }
        return map;
    }

    private long countRetailersWithOutstanding() {
        return financeRepo.topRetailersByOutstanding(Integer.MAX_VALUE).size();
    }

    private List<MonthlyTrendDto> buildMonthlyTrend(LocalDate dateFrom, LocalDate dateTo) {
        List<Object[]> rows = financeRepo.monthlyTrend(dateFrom, dateTo);
        List<MonthlyTrendDto> result = new ArrayList<>(rows.size());
        for (Object[] row : rows) {
            int yr = ((Number) row[0]).intValue();
            int mo = ((Number) row[1]).intValue();
            BigDecimal bs = toBigDecimal(row[2]);
            BigDecimal rec = toBigDecimal(row[3]);
            BigDecimal rch = toBigDecimal(row[4]);
            result.add(new MonthlyTrendDto(yr, mo, bs, rec, rch, bs.subtract(rec)));
        }
        return result;
    }

    private List<TopRetailerDto> buildTopByReceived() {
        List<Object[]> rows = financeRepo.topRetailersByReceived(TOP_RETAILERS_LIMIT);
        List<TopRetailerDto> result = new ArrayList<>(rows.size());
        for (Object[] row : rows) {
            result.add(new TopRetailerDto(
                    ((Number) row[0]).longValue(), (String) row[1], (String) row[2], toBigDecimal(row[3])));
        }
        return result;
    }

    private List<TopRetailerDto> buildTopByOutstanding() {
        List<Object[]> rows = financeRepo.topRetailersByOutstanding(TOP_RETAILERS_LIMIT);
        List<TopRetailerDto> result = new ArrayList<>(rows.size());
        for (Object[] row : rows) {
            result.add(new TopRetailerDto(
                    ((Number) row[0]).longValue(), (String) row[1], (String) row[2], toBigDecimal(row[3])));
        }
        return result;
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) return BigDecimal.ZERO;
        if (value instanceof BigDecimal bd) return bd;
        return new BigDecimal(value.toString());
    }

    private int financialYearStartYear(LocalDate date) {
        return date.getMonthValue() >= 4 ? date.getYear() : date.getYear() - 1;
    }
}
