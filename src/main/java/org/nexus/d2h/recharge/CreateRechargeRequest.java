package org.nexus.d2h.recharge;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import org.nexus.d2h.finance.PaymentMethod;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateRechargeRequest(
        @NotNull Long retailerId,
        Long assetId,
        @NotNull LocalDate rechargeDate,
        @NotNull @DecimalMin(value = "0.01", message = "Amount must be greater than zero") BigDecimal amount,
        @NotNull RechargeType rechargeType,
        PaymentMethod paymentMethod,
        String paymentReference,
        String externalReference,
        String servicePeriod,
        String description,
        String remarks,
        String reference
) {}
