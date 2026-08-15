package org.nexus.d2h.report;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PeriodReportDto(
        LocalDate dateFrom,
        LocalDate dateTo,
        BigDecimal boxSales,
        BigDecimal received,
        BigDecimal outstanding,
        BigDecimal recharge,
        long transactionCount
) {}
