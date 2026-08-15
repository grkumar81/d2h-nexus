package org.nexus.d2h.finance;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
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

    // ── Date-range aggregates for dashboard ───────────────────────────────────

    @Query("""
            SELECT COALESCE(SUM(t.amount), 0)
            FROM FinancialTransaction t
            WHERE t.tenant.id = :tenantId
              AND t.transactionType = org.nexus.d2h.finance.TransactionType.BOX_SALE
              AND t.transactionStatus = org.nexus.d2h.finance.TransactionStatus.POSTED
              AND t.transactionDate >= :dateFrom AND t.transactionDate <= :dateTo
            """)
    BigDecimal sumBoxSalesByTenantAndDateRange(@Param("tenantId") Long tenantId,
                                               @Param("dateFrom") LocalDate dateFrom,
                                               @Param("dateTo") LocalDate dateTo);

    @Query("""
            SELECT COALESCE(SUM(t.amount), 0)
            FROM FinancialTransaction t
            WHERE t.tenant.id = :tenantId
              AND t.transactionType = org.nexus.d2h.finance.TransactionType.PAYMENT_RECEIVED
              AND t.transactionStatus = org.nexus.d2h.finance.TransactionStatus.POSTED
              AND t.transactionDate >= :dateFrom AND t.transactionDate <= :dateTo
            """)
    BigDecimal sumPaymentsReceivedByTenantAndDateRange(@Param("tenantId") Long tenantId,
                                                       @Param("dateFrom") LocalDate dateFrom,
                                                       @Param("dateTo") LocalDate dateTo);

    @Query("""
            SELECT COALESCE(SUM(t.amount), 0)
            FROM FinancialTransaction t
            WHERE t.tenant.id = :tenantId
              AND t.transactionType = org.nexus.d2h.finance.TransactionType.RECHARGE
              AND t.transactionStatus = org.nexus.d2h.finance.TransactionStatus.POSTED
              AND t.transactionDate >= :dateFrom AND t.transactionDate <= :dateTo
            """)
    BigDecimal sumRechargeByTenantAndDateRange(@Param("tenantId") Long tenantId,
                                               @Param("dateFrom") LocalDate dateFrom,
                                               @Param("dateTo") LocalDate dateTo);

    @Query("""
            SELECT COUNT(t)
            FROM FinancialTransaction t
            WHERE t.tenant.id = :tenantId
              AND t.transactionStatus = org.nexus.d2h.finance.TransactionStatus.POSTED
              AND t.transactionDate >= :dateFrom AND t.transactionDate <= :dateTo
            """)
    long countPostedByTenantAndDateRange(@Param("tenantId") Long tenantId,
                                         @Param("dateFrom") LocalDate dateFrom,
                                         @Param("dateTo") LocalDate dateTo);

    // ── Monthly trend (native SQL for YEAR/MONTH functions) ───────────────────

    @Query(value = """
            SELECT YEAR(transaction_date) AS yr, MONTH(transaction_date) AS mo,
                   SUM(CASE WHEN transaction_type = 'BOX_SALE'        THEN amount ELSE 0 END) AS boxSales,
                   SUM(CASE WHEN transaction_type = 'PAYMENT_RECEIVED' THEN amount ELSE 0 END) AS received,
                   SUM(CASE WHEN transaction_type = 'RECHARGE'         THEN amount ELSE 0 END) AS recharge
            FROM financial_transactions
            WHERE tenant_id = :tenantId
              AND transaction_status = 'POSTED'
              AND transaction_date >= :dateFrom AND transaction_date <= :dateTo
            GROUP BY YEAR(transaction_date), MONTH(transaction_date)
            ORDER BY yr, mo
            """, nativeQuery = true)
    List<Object[]> monthlyTrend(@Param("tenantId") Long tenantId,
                                @Param("dateFrom") LocalDate dateFrom,
                                @Param("dateTo") LocalDate dateTo);

    // ── Top retailers by received / outstanding ────────────────────────────────

    @Query(value = """
            SELECT r.id, r.retailer_code, r.retailer_name,
                   COALESCE(SUM(CASE WHEN t.transaction_type = 'PAYMENT_RECEIVED' THEN t.amount ELSE 0 END), 0) AS received,
                   COALESCE(SUM(CASE WHEN t.transaction_type = 'BOX_SALE'         THEN t.amount ELSE 0 END), 0) AS boxSales
            FROM retailers r
            LEFT JOIN financial_transactions t
                   ON t.retailer_id = r.id AND t.tenant_id = r.tenant_id AND t.transaction_status = 'POSTED'
            WHERE r.tenant_id = :tenantId
            GROUP BY r.id, r.retailer_code, r.retailer_name
            ORDER BY received DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<Object[]> topRetailersByReceived(@Param("tenantId") Long tenantId, @Param("limit") int limit);

    @Query(value = """
            SELECT r.id, r.retailer_code, r.retailer_name,
                   COALESCE(SUM(CASE WHEN t.transaction_type = 'BOX_SALE'         THEN t.amount ELSE 0 END), 0)
                 - COALESCE(SUM(CASE WHEN t.transaction_type = 'PAYMENT_RECEIVED' THEN t.amount ELSE 0 END), 0) AS outstanding
            FROM retailers r
            LEFT JOIN financial_transactions t
                   ON t.retailer_id = r.id AND t.tenant_id = r.tenant_id AND t.transaction_status = 'POSTED'
            WHERE r.tenant_id = :tenantId
            GROUP BY r.id, r.retailer_code, r.retailer_name
            HAVING outstanding > 0
            ORDER BY outstanding DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<Object[]> topRetailersByOutstanding(@Param("tenantId") Long tenantId, @Param("limit") int limit);
}
