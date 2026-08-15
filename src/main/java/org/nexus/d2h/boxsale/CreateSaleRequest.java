package org.nexus.d2h.boxsale;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record CreateSaleRequest(
        @NotNull Long retailerId,
        @NotNull LocalDate transactionDate,
        @NotEmpty @Valid List<SaleItemRequest> items,
        String reference,
        String remarks
) {
    public record SaleItemRequest(
            @NotNull Long assetId,
            @NotNull @Positive BigDecimal unitPrice
    ) {}
}
