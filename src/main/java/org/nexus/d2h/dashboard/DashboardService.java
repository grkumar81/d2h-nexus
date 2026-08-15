package org.nexus.d2h.dashboard;

import lombok.extern.slf4j.Slf4j;
import org.nexus.d2h.asset.AssetRepository;
import org.nexus.d2h.asset.AssetStatus;
import org.nexus.d2h.common.BusinessException;
import org.nexus.d2h.common.ResourceNotFoundException;
import org.nexus.d2h.finance.FinancialTransactionRepository;
import org.nexus.d2h.recharge.RechargeTransactionRepository;
import org.nexus.d2h.retailer.RetailerRepository;
import org.nexus.d2h.retailer.RetailerStatus;
import org.nexus.d2h.tenant.Tenant;
import org.nexus.d2h.tenant.TenantContext;
import org.nexus.d2h.tenant.TenantRepository;
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
    private final TenantRepository tenantRepository;

    public DashboardService(FinancialTransactionRepository financeRepo,
                            RechargeTransactionRepository rechargeRepo,
                            AssetRepository assetRepo,
                            RetailerRepository retailerRepo,
                            TenantRepository tenantRepository) {
        this.financeRepo = financeRepo;
        this.rechargeRepo = rechargeRepo;
        this.assetRepo = assetRepo;
        this.retailerRepo = retailerRepo;
        this.tenantRepository = tenantRepository;
    }

    @Transactional(readOnly = true)
    public DashboardDto getDashboard(Integer fyYear) {
        Tenant tenant = resolveTenant();
        Long tenantId = tenant.getId();

        // Determine financial year boundaries (April–March)
        int currentYear = LocalDate.now().getYear();
        int startYear = (fyYear != null) ? fyYear : financialYearStartYear(LocalDate.now());
        LocalDate dateFrom = LocalDate.of(startYear, Month.APRIL, 1);
        LocalDate dateTo = LocalDate.of(startYear + 1, Month.MARCH, 31);

        // ── Financial KPIs ────────────────────────────────────────────────────
        BigDecimal boxSales = financeRepo.sumBoxSalesByTenantAndDateRange(tenantId, dateFrom, dateTo);
        BigDecimal received = financeRepo.sumPaymentsReceivedByTenantAndDateRange(tenantId, dateFrom, dateTo);
        BigDecimal recharge = financeRepo.sumRechargeByTenantAndDateRange(tenantId, dateFrom, dateTo);
        long txCount = financeRepo.countPostedByTenantAndDateRange(tenantId, dateFrom, dateTo);
        BigDecimal outstanding = boxSales.subtract(received);

        // ── Asset KPIs ────────────────────────────────────────────────────────
        long totalAssets = assetRepo.countByTenantId(tenantId);
        Map<AssetStatus, Long> assetCounts = assetCountsByStatus(tenantId);

        // ── Retailer KPIs ─────────────────────────────────────────────────────
        long totalRetailers = retailerRepo.countByTenantId(tenantId);
        long activeRetailers = retailerRepo.countByTenantIdAndStatus(tenantId, RetailerStatus.ACTIVE);
        long inactiveRetailers = retailerRepo.countByTenantIdAndStatus(tenantId, RetailerStatus.INACTIVE);
        long retailersWithOutstanding = countRetailersWithOutstanding(tenantId);

        // ── Monthly trend ─────────────────────────────────────────────────────
        List<MonthlyTrendDto> trend = buildMonthlyTrend(tenantId, dateFrom, dateTo);

        // ── Top retailers ─────────────────────────────────────────────────────
        List<TopRetailerDto> topByReceived = buildTopByReceived(tenantId);
        List<TopRetailerDto> topByOutstanding = buildTopByOutstanding(tenantId);

        log.debug("Dashboard loaded for tenant={} fy={}/{}", tenant.getTenantCode(), startYear, startYear + 1);

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

    // ── Private helpers ───────────────────────────────────────────────────────

    private Map<AssetStatus, Long> assetCountsByStatus(Long tenantId) {
        Map<AssetStatus, Long> map = new EnumMap<>(AssetStatus.class);
        for (Object[] row : assetRepo.countByStatusForTenant(tenantId)) {
            AssetStatus status = (AssetStatus) row[0];
            Long count = (Long) row[1];
            map.put(status, count);
        }
        return map;
    }

    private long countRetailersWithOutstanding(Long tenantId) {
        List<Object[]> rows = financeRepo.topRetailersByOutstanding(tenantId, Integer.MAX_VALUE);
        return rows.size();
    }

    private List<MonthlyTrendDto> buildMonthlyTrend(Long tenantId, LocalDate dateFrom, LocalDate dateTo) {
        List<Object[]> rows = financeRepo.monthlyTrend(tenantId, dateFrom, dateTo);
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

    private List<TopRetailerDto> buildTopByReceived(Long tenantId) {
        List<Object[]> rows = financeRepo.topRetailersByReceived(tenantId, TOP_RETAILERS_LIMIT);
        List<TopRetailerDto> result = new ArrayList<>(rows.size());
        for (Object[] row : rows) {
            result.add(new TopRetailerDto(
                    ((Number) row[0]).longValue(),
                    (String) row[1],
                    (String) row[2],
                    toBigDecimal(row[3])
            ));
        }
        return result;
    }

    private List<TopRetailerDto> buildTopByOutstanding(Long tenantId) {
        List<Object[]> rows = financeRepo.topRetailersByOutstanding(tenantId, TOP_RETAILERS_LIMIT);
        List<TopRetailerDto> result = new ArrayList<>(rows.size());
        for (Object[] row : rows) {
            result.add(new TopRetailerDto(
                    ((Number) row[0]).longValue(),
                    (String) row[1],
                    (String) row[2],
                    toBigDecimal(row[3])
            ));
        }
        return result;
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) return BigDecimal.ZERO;
        if (value instanceof BigDecimal bd) return bd;
        return new BigDecimal(value.toString());
    }

    private int financialYearStartYear(LocalDate date) {
        // Financial year: April 1 – March 31
        return date.getMonthValue() >= 4 ? date.getYear() : date.getYear() - 1;
    }

    private Tenant resolveTenant() {
        String tenantCode = TenantContext.getCurrentTenant();
        if (tenantCode == null || tenantCode.isBlank()) {
            throw new BusinessException("TENANT_CONTEXT_MISSING", "Tenant context is not set");
        }
        return tenantRepository.findByTenantCode(tenantCode)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", tenantCode));
    }
}
