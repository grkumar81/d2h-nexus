package org.nexus.d2h.report;

import java.math.BigDecimal;

public record RetailerReportDto(
        Long retailerId,
        String retailerCode,
        String retailerName,
        BigDecimal boxSales,
        BigDecimal received,
        BigDecimal outstanding,
        BigDecimal recharge
) {}
