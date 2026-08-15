package org.nexus.d2h.recharge;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RetailerRechargeSummaryDto(
        Long retailerId,
        String retailerCode,
        String retailerName,
        long rechargeCount,
        BigDecimal totalRecharge,
        BigDecimal successRecharge,
        LocalDate lastRechargeDate,
        BigDecimal lastRechargeAmount
) {}
