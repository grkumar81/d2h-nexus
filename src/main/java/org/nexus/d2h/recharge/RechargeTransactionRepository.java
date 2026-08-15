package org.nexus.d2h.recharge;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

public interface RechargeTransactionRepository
        extends JpaRepository<RechargeTransaction, Long>, JpaSpecificationExecutor<RechargeTransaction> {

    boolean existsByReference(String reference);

    @Query("""
            SELECT COALESCE(SUM(r.amount), 0)
            FROM RechargeTransaction r
            WHERE r.retailer.id = :retailerId
              AND r.rechargeStatus = org.nexus.d2h.recharge.RechargeStatus.SUCCESS
            """)
    BigDecimal sumSuccessByRetailer(@Param("retailerId") Long retailerId);

    @Query("""
            SELECT COALESCE(SUM(r.amount), 0)
            FROM RechargeTransaction r
            WHERE r.retailer.id = :retailerId
            """)
    BigDecimal sumTotalByRetailer(@Param("retailerId") Long retailerId);

    @Query("""
            SELECT COUNT(r)
            FROM RechargeTransaction r
            WHERE r.retailer.id = :retailerId
            """)
    long countByRetailer(@Param("retailerId") Long retailerId);

    @Query("""
            SELECT MAX(r.rechargeDate)
            FROM RechargeTransaction r
            WHERE r.retailer.id = :retailerId
            """)
    LocalDate lastRechargeDateByRetailer(@Param("retailerId") Long retailerId);

    @Query("""
            SELECT r.amount
            FROM RechargeTransaction r
            WHERE r.retailer.id = :retailerId
            ORDER BY r.rechargeDate DESC, r.id DESC
            LIMIT 1
            """)
    BigDecimal lastRechargeAmountByRetailer(@Param("retailerId") Long retailerId);

    @Query("""
            SELECT COALESCE(SUM(r.amount), 0)
            FROM RechargeTransaction r
            WHERE r.rechargeStatus = org.nexus.d2h.recharge.RechargeStatus.SUCCESS
              AND (:dateFrom IS NULL OR r.rechargeDate >= :dateFrom)
              AND (:dateTo   IS NULL OR r.rechargeDate <= :dateTo)
            """)
    BigDecimal sumSuccess(@Param("dateFrom") LocalDate dateFrom,
                          @Param("dateTo") LocalDate dateTo);

    @Query("""
            SELECT COALESCE(SUM(r.amount), 0)
            FROM RechargeTransaction r
            WHERE r.rechargeStatus = org.nexus.d2h.recharge.RechargeStatus.FAILED
              AND (:dateFrom IS NULL OR r.rechargeDate >= :dateFrom)
              AND (:dateTo   IS NULL OR r.rechargeDate <= :dateTo)
            """)
    BigDecimal sumFailed(@Param("dateFrom") LocalDate dateFrom,
                         @Param("dateTo") LocalDate dateTo);

    @Query("""
            SELECT COALESCE(SUM(r.amount), 0)
            FROM RechargeTransaction r
            WHERE r.rechargeStatus = org.nexus.d2h.recharge.RechargeStatus.REVERSED
              AND (:dateFrom IS NULL OR r.rechargeDate >= :dateFrom)
              AND (:dateTo   IS NULL OR r.rechargeDate <= :dateTo)
            """)
    BigDecimal sumReversed(@Param("dateFrom") LocalDate dateFrom,
                           @Param("dateTo") LocalDate dateTo);

    @Query("""
            SELECT COALESCE(SUM(r.amount), 0)
            FROM RechargeTransaction r
            WHERE (:dateFrom IS NULL OR r.rechargeDate >= :dateFrom)
              AND (:dateTo   IS NULL OR r.rechargeDate <= :dateTo)
            """)
    BigDecimal sumTotal(@Param("dateFrom") LocalDate dateFrom,
                        @Param("dateTo") LocalDate dateTo);

    @Query("""
            SELECT COUNT(r)
            FROM RechargeTransaction r
            WHERE (:dateFrom IS NULL OR r.rechargeDate >= :dateFrom)
              AND (:dateTo   IS NULL OR r.rechargeDate <= :dateTo)
            """)
    long countAll(@Param("dateFrom") LocalDate dateFrom,
                  @Param("dateTo") LocalDate dateTo);
}
