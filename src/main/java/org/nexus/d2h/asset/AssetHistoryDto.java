package org.nexus.d2h.asset;

import java.time.Instant;

public record AssetHistoryDto(
        Long id,
        Long assetId,
        AssetStatus fromStatus,
        AssetStatus toStatus,
        Long retailerId,
        String retailerCode,
        String changedBy,
        String remarks,
        Instant changedAt
) {
    public static AssetHistoryDto from(StbAssetHistory h) {
        return new AssetHistoryDto(
                h.getId(),
                h.getAsset().getId(),
                h.getFromStatus(),
                h.getToStatus(),
                h.getRetailer() != null ? h.getRetailer().getId() : null,
                h.getRetailer() != null ? h.getRetailer().getRetailerCode() : null,
                h.getChangedBy(),
                h.getRemarks(),
                h.getChangedAt()
        );
    }
}
