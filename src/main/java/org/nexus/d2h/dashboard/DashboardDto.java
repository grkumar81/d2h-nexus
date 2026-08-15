package org.nexus.d2h.dashboard;

import java.math.BigDecimal;
import java.util.List;

public record DashboardDto(
        // Financial KPIs
        BigDecimal totalBoxSales,
        BigDecimal totalReceived,
        BigDecimal totalOutstanding,
        BigDecimal totalRecharge,
        long transactionCount,

        // Asset KPIs
        long totalAssets,
        long availableAssets,
        long allocatedAssets,
        long soldAssets,
        long activatedAssets,
        long returnedAssets,
        long damagedAssets,
        long lostAssets,

        // Retailer KPIs
        long totalRetailers,
        long activeRetailers,
        long inactiveRetailers,
        long retailersWithOutstanding,

        // Charts
        List<MonthlyTrendDto> monthlyTrend,
        List<TopRetailerDto> topByReceived,
        List<TopRetailerDto> topByOutstanding,

        // Period
        int financialYearStart,
        int financialYearEnd
) {}
