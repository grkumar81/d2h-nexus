package org.nexus.d2h.asset;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.nexus.d2h.common.BaseEntity;
import org.nexus.d2h.retailer.Retailer;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "stb_assets")
public class StbAsset extends BaseEntity {

    @Column(name = "serial_number", nullable = false, length = 100)
    private String serialNumber;

    @Column(name = "box_number", length = 100)
    private String boxNumber;

    @Column(length = 100)
    private String model;

    @Column(length = 100)
    private String manufacturer;

    @Column(length = 100)
    private String batch;

    @Column(name = "purchase_date")
    private LocalDate purchaseDate;

    @Column(name = "purchase_cost", precision = 15, scale = 2)
    private BigDecimal purchaseCost;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AssetStatus status = AssetStatus.AVAILABLE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "retailer_id")
    private Retailer retailer;

    @Column(name = "tagging_date")
    private LocalDate taggingDate;

    @Column(name = "sale_date")
    private LocalDate saleDate;

    @Column(name = "activation_date")
    private LocalDate activationDate;

    @Column(name = "return_date")
    private LocalDate returnDate;

    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    @Version
    @Column(nullable = false)
    private Long version = 0L;
}
