package org.nexus.d2h.dashboard;

import java.math.BigDecimal;

public record MonthlyTrendDto(
        int year,
        int month,
        BigDecimal boxSales,
        BigDecimal received,
        BigDecimal recharge,
        BigDecimal outstanding
) {}
