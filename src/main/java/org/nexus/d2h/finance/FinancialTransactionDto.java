package org.nexus.d2h.finance;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record FinancialTransactionDto(
        Long id,
        Long retailerId,
        String retailerCode,
        String retailerName,
        TransactionType transactionType,
        TransactionStatus transactionStatus,
        LocalDate transactionDate,
        BigDecimal amount,
        PaymentMethod paymentMethod,
        String reference,
        String paymentReference,
        String description,
        String remarks,
        TransactionSource source,
        Long saleId,
        Long reversedById,
        Long reversalOfId,
        String createdBy,
        String updatedBy,
        Instant createdAt,
        Instant updatedAt
) {
    public static FinancialTransactionDto from(FinancialTransaction t) {
        return new FinancialTransactionDto(
                t.getId(),
                t.getRetailer().getId(),
                t.getRetailer().getRetailerCode(),
                t.getRetailer().getRetailerName(),
                t.getTransactionType(),
                t.getTransactionStatus(),
                t.getTransactionDate(),
                t.getAmount(),
                t.getPaymentMethod(),
                t.getReference(),
                t.getPaymentReference(),
                t.getDescription(),
                t.getRemarks(),
                t.getSource(),
                t.getSale() != null ? t.getSale().getId() : null,
                t.getReversedBy() != null ? t.getReversedBy().getId() : null,
                t.getReversalOf() != null ? t.getReversalOf().getId() : null,
                t.getCreatedBy(),
                t.getUpdatedBy(),
                t.getCreatedAt(),
                t.getUpdatedAt()
        );
    }
}
