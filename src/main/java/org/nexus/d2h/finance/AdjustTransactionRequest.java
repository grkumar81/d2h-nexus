package org.nexus.d2h.finance;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

public record AdjustTransactionRequest(
        @NotNull BigDecimal adjustmentAmount,
        @NotBlank String reason
) {}
