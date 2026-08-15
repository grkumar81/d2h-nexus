package org.nexus.d2h.finance;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.nexus.d2h.boxsale.StbSale;
import org.nexus.d2h.common.BaseEntity;
import org.nexus.d2h.retailer.Retailer;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "financial_transactions")
public class FinancialTransaction extends BaseEntity {

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private Long tenantId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "retailer_id", nullable = false)
    private Retailer retailer;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 30)
    private TransactionType transactionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_status", nullable = false, length = 20)
    private TransactionStatus transactionStatus = TransactionStatus.POSTED;

    @Column(name = "transaction_date", nullable = false)
    private LocalDate transactionDate;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", length = 30)
    private PaymentMethod paymentMethod;

    @Column(length = 100)
    private String reference;

    @Column(name = "payment_reference", length = 100)
    private String paymentReference;

    @Column(length = 500)
    private String description;

    @Column(length = 500)
    private String remarks;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TransactionSource source = TransactionSource.MANUAL;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sale_id")
    private StbSale sale;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reversed_by_id")
    private FinancialTransaction reversedBy;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reversal_of_id")
    private FinancialTransaction reversalOf;

    @Column(name = "updated_by", length = 100)
    private String updatedBy;
}
