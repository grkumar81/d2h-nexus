package org.nexus.d2h.boxsale;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record SaleDto(
        Long id,
        Long retailerId,
        String retailerCode,
        String retailerName,
        LocalDate transactionDate,
        BigDecimal totalAmount,
        PaymentStatus paymentStatus,
        String reference,
        String remarks,
        List<SaleItemDto> items,
        String createdBy,
        Instant createdAt
) {
    public static SaleDto from(StbSale s) {
        return new SaleDto(
                s.getId(),
                s.getRetailer().getId(),
                s.getRetailer().getRetailerCode(),
                s.getRetailer().getRetailerName(),
                s.getTransactionDate(),
                s.getTotalAmount(),
                s.getPaymentStatus(),
                s.getReference(),
                s.getRemarks(),
                s.getItems().stream().map(SaleItemDto::from).toList(),
                s.getCreatedBy(),
                s.getCreatedAt()
        );
    }
}
