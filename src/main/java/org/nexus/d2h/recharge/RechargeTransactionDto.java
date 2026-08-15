package org.nexus.d2h.recharge;

import org.nexus.d2h.finance.PaymentMethod;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record RechargeTransactionDto(
        Long id,
        Long retailerId,
        String retailerCode,
        String retailerName,
        Long assetId,
        String assetSerial,
        String reference,
        String externalReference,
        LocalDate rechargeDate,
        BigDecimal amount,
        RechargeType rechargeType,
        RechargeStatus rechargeStatus,
        PaymentMethod paymentMethod,
        String paymentReference,
        String servicePeriod,
        String description,
        String remarks,
        RechargeSource source,
        Long reversedById,
        Long reversalOfId,
        String createdBy,
        String updatedBy,
        Instant createdAt,
        Instant updatedAt
) {
    public static RechargeTransactionDto from(RechargeTransaction r) {
        return new RechargeTransactionDto(
                r.getId(),
                r.getRetailer().getId(),
                r.getRetailer().getRetailerCode(),
                r.getRetailer().getRetailerName(),
                r.getAsset() != null ? r.getAsset().getId() : null,
                r.getAsset() != null ? r.getAsset().getSerialNumber() : null,
                r.getReference(),
                r.getExternalReference(),
                r.getRechargeDate(),
                r.getAmount(),
                r.getRechargeType(),
                r.getRechargeStatus(),
                r.getPaymentMethod(),
                r.getPaymentReference(),
                r.getServicePeriod(),
                r.getDescription(),
                r.getRemarks(),
                r.getSource(),
                r.getReversedBy() != null ? r.getReversedBy().getId() : null,
                r.getReversalOf() != null ? r.getReversalOf().getId() : null,
                r.getCreatedBy(),
                r.getUpdatedBy(),
                r.getCreatedAt(),
                r.getUpdatedAt()
        );
    }
}
