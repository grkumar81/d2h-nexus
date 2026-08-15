package org.nexus.d2h.dashboard;

import java.math.BigDecimal;

public record TopRetailerDto(
        Long retailerId,
        String retailerCode,
        String retailerName,
        BigDecimal amount
) {}
