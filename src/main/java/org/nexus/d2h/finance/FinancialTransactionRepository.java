package org.nexus.d2h.finance;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface FinancialTransactionRepository
        extends JpaRepository<FinancialTransaction, Long>, JpaSpecificationExecutor<FinancialTransaction> {

    boolean existsByReference(String reference);

    boolean existsBySaleId(Long saleId);

    @Query("""
            SELECT COALESCE(SUM(t.amount), 0)
            FROM FinancialTransaction t
            WHERE t.retailer.id = :retailerId
              AND t.transactionType = org.nexus.d2h.finance.TransactionType.BOX_SALE
              AND t.transactionStatus = org.nexus.d2h.finance.TransactionStatus.POSTED
            """)
    BigDecimal sumBoxSalesByRetailer(@Param("retailerId") Long retailerId);

    @Query("""
            SELECT COALESCE(SUM(t.amount), 0)
            FROM FinancialTransaction t
            WHERE t.retailer.id = :retailerId
              AND t.transactionType = org.nexus.d2h.finance.TransactionType.PAYMENT_RECEIVED
              AND t.transactionStatus = org.nexus.d2h.finance.TransactionStatus.POSTED
            """)
    BigDecimal sumPaymentsReceivedByRetailer(@Param("retailerId") Long retailerId);

    @Query("""
            SELECT COALESCE(SUM(t.amount), 0)
            FROM FinancialTransaction t
            WHERE t.retailer.id = :retailerId
              AND t.transactionType = org.nexus.d2h.finance.TransactionType.RECHARGE
              AND t.transactionStatus = org.nexus.d2h.finance.TransactionStatus.POSTED
            """)
    BigDecimal sumRechargeByRetailer(@Param("retailerId") Long retailerId);

    @Query("""
            SELECT COALESCE(SUM(t.amount), 0)
            FROM FinancialTransaction t
            WHERE t.transactionType = org.nexus.d2h.finance.TransactionType.BOX_SALE
              AND t.transactionStatus = org.nexus.d2h.finance.TransactionStatus.POSTED
            """)
    BigDecimal sumBoxSales();

    @Query("""
            SELECT COALESCE(SUM(t.amount), 0)
            FROM FinancialTransaction t
            WHERE t.transactionType = org.nexus.d2h.finance.TransactionType.PAYMENT_RECEIVED
              AND t.transactionStatus = org.nexus.d2h.finance.TransactionStatus.POSTED
            """)
    BigDecimal sumPaymentsReceived();

    @Query("""
            SELECT COALESCE(SUM(t.amount), 0)
            FROM FinancialTransaction t
            WHERE t.transactionType = org.nexus.d2h.finance.TransactionType.RECHARGE
              AND t.transactionStatus = org.nexus.d2h.finance.TransactionStatus.POSTED
            """)
    BigDecimal sumRecharge();

    @Query("""
            SELECT COUNT(t)
            FROM FinancialTransaction t
            WHERE t.transactionStatus = org.nexus.d2h.finance.TransactionStatus.POSTED
            """)
    long countPosted();

    @Query("""
            SELECT COALESCE(SUM(t.amount), 0)
            FROM FinancialTransaction t
            WHERE t.transactionType = org.nexus.d2h.finance.TransactionType.BOX_SALE
              AND t.transactionStatus = org.nexus.d2h.finance.TransactionStatus.POSTED
              AND t.transactionDate >= :dateFrom AND t.transactionDate <= :dateTo
            """)
    BigDecimal sumBoxSalesByDateRange(@Param("dateFrom") LocalDate dateFrom,
                                      @Param("dateTo") LocalDate dateTo);

    @Query("""
            SELECT COALESCE(SUM(t.amount), 0)
            FROM FinancialTransaction t
            WHERE t.transactionType = org.nexus.d2h.finance.TransactionType.PAYMENT_RECEIVED
              AND t.transactionStatus = org.nexus.d2h.finance.TransactionStatus.POSTED
              AND t.transactionDate >= :dateFrom AND t.transactionDate <= :dateTo
            """)
    BigDecimal sumPaymentsReceivedByDateRange(@Param("dateFrom") LocalDate dateFrom,
                                              @Param("dateTo") LocalDate dateTo);

    @Query("""
            SELECT COALESCE(SUM(t.amount), 0)
            FROM FinancialTransaction t
            WHERE t.transactionType = org.nexus.d2h.finance.TransactionType.RECHARGE
              AND t.transactionStatus = org.nexus.d2h.finance.TransactionStatus.POSTED
              AND t.transactionDate >= :dateFrom AND t.transactionDate <= :dateTo
            """)
    BigDecimal sumRechargeByDateRange(@Param("dateFrom") LocalDate dateFrom,
                                      @Param("dateTo") LocalDate dateTo);

    @Query("""
            SELECT COUNT(t)
            FROM FinancialTransaction t
            WHERE t.transactionStatus = org.nexus.d2h.finance.TransactionStatus.POSTED
              AND t.transactionDate >= :dateFrom AND t.transactionDate <= :dateTo
            """)
    long countPostedByDateRange(@Param("dateFrom") LocalDate dateFrom,
                                @Param("dateTo") LocalDate dateTo);

    @Query(value = """
            SELECT YEAR(transaction_date) AS yr, MONTH(transaction_date) AS mo,
                   SUM(CASE WHEN transaction_type = 'BOX_SALE'        THEN amount ELSE 0 END) AS boxSales,
                   SUM(CASE WHEN transaction_type = 'PAYMENT_RECEIVED' THEN amount ELSE 0 END) AS received,
                   SUM(CASE WHEN transaction_type = 'RECHARGE'         THEN amount ELSE 0 END) AS recharge
            FROM financial_transactions
            WHERE transaction_status = 'POSTED'
              AND transaction_date >= :dateFrom AND transaction_date <= :dateTo
            GROUP BY YEAR(transaction_date), MONTH(transaction_date)
            ORDER BY yr, mo
            """, nativeQuery = true)
    List<Object[]> monthlyTrend(@Param("dateFrom") LocalDate dateFrom,
                                @Param("dateTo") LocalDate dateTo);

    @Query(value = """
            SELECT r.id, r.retailer_code, r.retailer_name,
                   COALESCE(SUM(CASE WHEN t.transaction_type = 'PAYMENT_RECEIVED' THEN t.amount ELSE 0 END), 0) AS received,
                   COALESCE(SUM(CASE WHEN t.transaction_type = 'BOX_SALE'         THEN t.amount ELSE 0 END), 0) AS boxSales
            FROM retailers r
            LEFT JOIN financial_transactions t
                   ON t.retailer_id = r.id AND t.transaction_status = 'POSTED'
            GROUP BY r.id, r.retailer_code, r.retailer_name
            ORDER BY received DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<Object[]> topRetailersByReceived(@Param("limit") int limit);

    @Query(value = """
            SELECT r.id, r.retailer_code, r.retailer_name,
                   COALESCE(SUM(CASE WHEN t.transaction_type = 'BOX_SALE'         THEN t.amount ELSE 0 END), 0)
                 - COALESCE(SUM(CASE WHEN t.transaction_type = 'PAYMENT_RECEIVED' THEN t.amount ELSE 0 END), 0) AS outstanding
            FROM retailers r
            LEFT JOIN financial_transactions t
                   ON t.retailer_id = r.id AND t.transaction_status = 'POSTED'
            GROUP BY r.id, r.retailer_code, r.retailer_name
            HAVING outstanding > 0
            ORDER BY outstanding DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<Object[]> topRetailersByOutstanding(@Param("limit") int limit);

    @Query(value = """
            SELECT r.id, r.retailer_code, r.retailer_name,
                   COALESCE(SUM(CASE WHEN t.transaction_type = 'BOX_SALE'         THEN t.amount ELSE 0 END), 0) AS boxSales,
                   COALESCE(SUM(CASE WHEN t.transaction_type = 'PAYMENT_RECEIVED' THEN t.amount ELSE 0 END), 0) AS received,
                   COALESCE(SUM(CASE WHEN t.transaction_type = 'RECHARGE'         THEN t.amount ELSE 0 END), 0) AS recharge
            FROM retailers r
            LEFT JOIN financial_transactions t
                   ON t.retailer_id = r.id AND t.transaction_status = 'POSTED'
                  AND (:dateFrom IS NULL OR t.transaction_date >= :dateFrom)
                  AND (:dateTo   IS NULL OR t.transaction_date <= :dateTo)
            GROUP BY r.id, r.retailer_code, r.retailer_name
            ORDER BY r.retailer_code
            """, nativeQuery = true)
    List<Object[]> allRetailerReport(@Param("dateFrom") LocalDate dateFrom,
                                     @Param("dateTo") LocalDate dateTo);
}
