package org.nexus.d2h.finance;

import java.math.BigDecimal;

public record RetailerFinanceSummaryDto(
        Long retailerId,
        String retailerCode,
        String retailerName,
        BigDecimal totalBoxSales,
        BigDecimal totalDue,
        BigDecimal totalReceived,
        BigDecimal outstanding,
        BigDecimal totalRecharge
) {}
