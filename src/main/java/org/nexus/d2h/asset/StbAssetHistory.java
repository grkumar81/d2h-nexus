package org.nexus.d2h.asset;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.nexus.d2h.retailer.Retailer;

import java.time.Instant;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "stb_asset_history")
public class StbAssetHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asset_id", nullable = false, updatable = false)
    private StbAsset asset;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private Long tenantId;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 20)
    private AssetStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false, length = 20)
    private AssetStatus toStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "retailer_id")
    private Retailer retailer;

    @Column(name = "changed_by", length = 100)
    private String changedBy;

    @Column(length = 500)
    private String remarks;

    @Column(name = "changed_at", nullable = false, updatable = false)
    private Instant changedAt;

    public StbAssetHistory(StbAsset asset, AssetStatus fromStatus, AssetStatus toStatus,
                           Retailer retailer, String changedBy, String remarks) {
        this.asset = asset;
        this.tenantId = asset.getTenantId();
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.retailer = retailer;
        this.changedBy = changedBy;
        this.remarks = remarks;
        this.changedAt = Instant.now();
    }
}
