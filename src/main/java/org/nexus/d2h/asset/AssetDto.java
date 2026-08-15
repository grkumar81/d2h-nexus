package org.nexus.d2h.asset;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record AssetDto(
        Long id,
        String serialNumber,
        String boxNumber,
        String model,
        String manufacturer,
        String batch,
        LocalDate purchaseDate,
        BigDecimal purchaseCost,
        AssetStatus status,
        Long retailerId,
        String retailerCode,
        String retailerName,
        LocalDate taggingDate,
        LocalDate saleDate,
        LocalDate activationDate,
        LocalDate returnDate,
        String createdBy,
        String updatedBy,
        Instant createdAt,
        Instant updatedAt
) {
    public static AssetDto from(StbAsset a) {
        return new AssetDto(
                a.getId(),
                a.getSerialNumber(),
                a.getBoxNumber(),
                a.getModel(),
                a.getManufacturer(),
                a.getBatch(),
                a.getPurchaseDate(),
                a.getPurchaseCost(),
                a.getStatus(),
                a.getRetailer() != null ? a.getRetailer().getId() : null,
                a.getRetailer() != null ? a.getRetailer().getRetailerCode() : null,
                a.getRetailer() != null ? a.getRetailer().getRetailerName() : null,
                a.getTaggingDate(),
                a.getSaleDate(),
                a.getActivationDate(),
                a.getReturnDate(),
                a.getCreatedBy(),
                a.getUpdatedBy(),
                a.getCreatedAt(),
                a.getUpdatedAt()
        );
    }
}
