package org.nexus.d2h.boxsale;

import java.math.BigDecimal;

public record SaleItemDto(
        Long id,
        Long assetId,
        String serialNumber,
        String boxNumber,
        BigDecimal unitPrice
) {
    public static SaleItemDto from(StbSaleItem item) {
        return new SaleItemDto(
                item.getId(),
                item.getAsset().getId(),
                item.getAsset().getSerialNumber(),
                item.getAsset().getBoxNumber(),
                item.getUnitPrice()
        );
    }
}
