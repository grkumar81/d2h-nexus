package org.nexus.d2h.finance;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateTransactionRequest(
        @NotNull Long retailerId,
        @NotNull TransactionType transactionType,
        @NotNull LocalDate transactionDate,
        @NotNull @Positive BigDecimal amount,
        PaymentMethod paymentMethod,
        @Size(max = 100) String reference,
        @Size(max = 100) String paymentReference,
        @Size(max = 500) String description,
        @Size(max = 500) String remarks
) {}
