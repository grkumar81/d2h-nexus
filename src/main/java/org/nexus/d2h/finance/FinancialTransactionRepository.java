package org.nexus.d2h.finance;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Optional;

public interface FinancialTransactionRepository
        extends JpaRepository<FinancialTransaction, Long>, JpaSpecificationExecutor<FinancialTransaction> {

    Optional<FinancialTransaction> findByIdAndTenantId(Long id, Long tenantId);

    boolean existsByTenantIdAndReference(Long tenantId, String reference);

    boolean existsByTenantIdAndSaleId(Long tenantId, Long saleId);

    // ── Aggregate queries for financial calculations ──────────────────────────

    /** Sum of POSTED BOX_SALE amounts for a retailer = Total Due */
    @Query("""
            SELECT COALESCE(SUM(t.amount), 0)
            FROM FinancialTransaction t
            WHERE t.tenant.id = :tenantId
              AND t.retailer.id = :retailerId
              AND t.transactionType = org.nexus.d2h.finance.TransactionType.BOX_SALE
              AND t.transactionStatus = org.nexus.d2h.finance.TransactionStatus.POSTED
            """)
    BigDecimal sumBoxSalesByRetailer(@Param("tenantId") Long tenantId, @Param("retailerId") Long retailerId);

    /** Sum of POSTED PAYMENT_RECEIVED amounts for a retailer = Total Received */
    @Query("""
            SELECT COALESCE(SUM(t.amount), 0)
            FROM FinancialTransaction t
            WHERE t.tenant.id = :tenantId
              AND t.retailer.id = :retailerId
              AND t.transactionType = org.nexus.d2h.finance.TransactionType.PAYMENT_RECEIVED
              AND t.transactionStatus = org.nexus.d2h.finance.TransactionStatus.POSTED
            """)
    BigDecimal sumPaymentsReceivedByRetailer(@Param("tenantId") Long tenantId, @Param("retailerId") Long retailerId);

    /** Sum of POSTED RECHARGE amounts for a retailer */
    @Query("""
            SELECT COALESCE(SUM(t.amount), 0)
            FROM FinancialTransaction t
            WHERE t.tenant.id = :tenantId
              AND t.retailer.id = :retailerId
              AND t.transactionType = org.nexus.d2h.finance.TransactionType.RECHARGE
              AND t.transactionStatus = org.nexus.d2h.finance.TransactionStatus.POSTED
            """)
    BigDecimal sumRechargeByRetailer(@Param("tenantId") Long tenantId, @Param("retailerId") Long retailerId);

    // ── Tenant-wide aggregates for dashboard/summary ─────────────────────────

    @Query("""
            SELECT COALESCE(SUM(t.amount), 0)
            FROM FinancialTransaction t
            WHERE t.tenant.id = :tenantId
              AND t.transactionType = org.nexus.d2h.finance.TransactionType.BOX_SALE
              AND t.transactionStatus = org.nexus.d2h.finance.TransactionStatus.POSTED
            """)
    BigDecimal sumBoxSalesByTenant(@Param("tenantId") Long tenantId);

    @Query("""
            SELECT COALESCE(SUM(t.amount), 0)
            FROM FinancialTransaction t
            WHERE t.tenant.id = :tenantId
              AND t.transactionType = org.nexus.d2h.finance.TransactionType.PAYMENT_RECEIVED
              AND t.transactionStatus = org.nexus.d2h.finance.TransactionStatus.POSTED
            """)
    BigDecimal sumPaymentsReceivedByTenant(@Param("tenantId") Long tenantId);

    @Query("""
            SELECT COALESCE(SUM(t.amount), 0)
            FROM FinancialTransaction t
            WHERE t.tenant.id = :tenantId
              AND t.transactionType = org.nexus.d2h.finance.TransactionType.RECHARGE
              AND t.transactionStatus = org.nexus.d2h.finance.TransactionStatus.POSTED
            """)
    BigDecimal sumRechargeByTenant(@Param("tenantId") Long tenantId);

    @Query("""
            SELECT COUNT(t)
            FROM FinancialTransaction t
            WHERE t.tenant.id = :tenantId
              AND t.transactionStatus = org.nexus.d2h.finance.TransactionStatus.POSTED
            """)
    long countPostedByTenant(@Param("tenantId") Long tenantId);
}
