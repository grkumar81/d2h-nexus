package org.nexus.d2h.finance;

import java.math.BigDecimal;

public record FinanceSummaryDto(
        BigDecimal totalDue,
        BigDecimal totalReceived,
        BigDecimal outstanding,
        BigDecimal totalRecharge,
        long transactionCount
) {}
