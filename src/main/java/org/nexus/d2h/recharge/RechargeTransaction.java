package org.nexus.d2h.recharge;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.nexus.d2h.asset.StbAsset;
import org.nexus.d2h.common.BaseEntity;
import org.nexus.d2h.retailer.Retailer;
import org.nexus.d2h.tenant.Tenant;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "recharge_transactions")
public class RechargeTransaction extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false, updatable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "retailer_id", nullable = false)
    private Retailer retailer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asset_id")
    private StbAsset asset;

    @Column(nullable = false, length = 100)
    private String reference;

    @Column(name = "external_reference", length = 100)
    private String externalReference;

    @Column(name = "recharge_date", nullable = false)
    private LocalDate rechargeDate;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "recharge_type", nullable = false, length = 30)
    private RechargeType rechargeType = RechargeType.REGULAR;

    @Enumerated(EnumType.STRING)
    @Column(name = "recharge_status", nullable = false, length = 20)
    private RechargeStatus rechargeStatus = RechargeStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", length = 30)
    private org.nexus.d2h.finance.PaymentMethod paymentMethod;

    @Column(name = "payment_reference", length = 100)
    private String paymentReference;

    @Column(name = "service_period", length = 100)
    private String servicePeriod;

    @Column(length = 500)
    private String description;

    @Column(length = 500)
    private String remarks;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RechargeSource source = RechargeSource.MANUAL;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reversed_by_id")
    private RechargeTransaction reversedBy;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reversal_of_id")
    private RechargeTransaction reversalOf;

    @Column(name = "updated_by", length = 100)
    private String updatedBy;
}
