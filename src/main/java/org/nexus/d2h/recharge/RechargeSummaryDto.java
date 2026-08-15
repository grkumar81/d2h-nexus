package org.nexus.d2h.recharge;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RechargeSummaryDto(
        long totalCount,
        BigDecimal totalAmount,
        BigDecimal successAmount,
        BigDecimal failedAmount,
        BigDecimal reversedAmount,
        LocalDate dateFrom,
        LocalDate dateTo
) {}
