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

    Optional<RechargeTransaction> findByIdAndTenantId(Long id, Long tenantId);

    boolean existsByTenantIdAndReference(Long tenantId, String reference);

    // ── Retailer-level aggregates ─────────────────────────────────────────────

    @Query("""
            SELECT COALESCE(SUM(r.amount), 0)
            FROM RechargeTransaction r
            WHERE r.tenant.id = :tenantId
              AND r.retailer.id = :retailerId
              AND r.rechargeStatus = org.nexus.d2h.recharge.RechargeStatus.SUCCESS
            """)
    BigDecimal sumSuccessByRetailer(@Param("tenantId") Long tenantId, @Param("retailerId") Long retailerId);

    @Query("""
            SELECT COALESCE(SUM(r.amount), 0)
            FROM RechargeTransaction r
            WHERE r.tenant.id = :tenantId
              AND r.retailer.id = :retailerId
            """)
    BigDecimal sumTotalByRetailer(@Param("tenantId") Long tenantId, @Param("retailerId") Long retailerId);

    @Query("""
            SELECT COUNT(r)
            FROM RechargeTransaction r
            WHERE r.tenant.id = :tenantId
              AND r.retailer.id = :retailerId
            """)
    long countByRetailer(@Param("tenantId") Long tenantId, @Param("retailerId") Long retailerId);

    @Query("""
            SELECT MAX(r.rechargeDate)
            FROM RechargeTransaction r
            WHERE r.tenant.id = :tenantId
              AND r.retailer.id = :retailerId
            """)
    LocalDate lastRechargeDateByRetailer(@Param("tenantId") Long tenantId, @Param("retailerId") Long retailerId);

    @Query("""
            SELECT r.amount
            FROM RechargeTransaction r
            WHERE r.tenant.id = :tenantId
              AND r.retailer.id = :retailerId
            ORDER BY r.rechargeDate DESC, r.id DESC
            LIMIT 1
            """)
    BigDecimal lastRechargeAmountByRetailer(@Param("tenantId") Long tenantId, @Param("retailerId") Long retailerId);

    // ── Tenant-level aggregates for summary ───────────────────────────────────

    @Query("""
            SELECT COALESCE(SUM(r.amount), 0)
            FROM RechargeTransaction r
            WHERE r.tenant.id = :tenantId
              AND r.rechargeStatus = org.nexus.d2h.recharge.RechargeStatus.SUCCESS
              AND (:dateFrom IS NULL OR r.rechargeDate >= :dateFrom)
              AND (:dateTo   IS NULL OR r.rechargeDate <= :dateTo)
            """)
    BigDecimal sumSuccessByTenant(@Param("tenantId") Long tenantId,
                                  @Param("dateFrom") LocalDate dateFrom,
                                  @Param("dateTo") LocalDate dateTo);

    @Query("""
            SELECT COALESCE(SUM(r.amount), 0)
            FROM RechargeTransaction r
            WHERE r.tenant.id = :tenantId
              AND r.rechargeStatus = org.nexus.d2h.recharge.RechargeStatus.FAILED
              AND (:dateFrom IS NULL OR r.rechargeDate >= :dateFrom)
              AND (:dateTo   IS NULL OR r.rechargeDate <= :dateTo)
            """)
    BigDecimal sumFailedByTenant(@Param("tenantId") Long tenantId,
                                 @Param("dateFrom") LocalDate dateFrom,
                                 @Param("dateTo") LocalDate dateTo);

    @Query("""
            SELECT COALESCE(SUM(r.amount), 0)
            FROM RechargeTransaction r
            WHERE r.tenant.id = :tenantId
              AND r.rechargeStatus = org.nexus.d2h.recharge.RechargeStatus.REVERSED
              AND (:dateFrom IS NULL OR r.rechargeDate >= :dateFrom)
              AND (:dateTo   IS NULL OR r.rechargeDate <= :dateTo)
            """)
    BigDecimal sumReversedByTenant(@Param("tenantId") Long tenantId,
                                   @Param("dateFrom") LocalDate dateFrom,
                                   @Param("dateTo") LocalDate dateTo);

    @Query("""
            SELECT COALESCE(SUM(r.amount), 0)
            FROM RechargeTransaction r
            WHERE r.tenant.id = :tenantId
              AND (:dateFrom IS NULL OR r.rechargeDate >= :dateFrom)
              AND (:dateTo   IS NULL OR r.rechargeDate <= :dateTo)
            """)
    BigDecimal sumTotalByTenant(@Param("tenantId") Long tenantId,
                                @Param("dateFrom") LocalDate dateFrom,
                                @Param("dateTo") LocalDate dateTo);

    @Query("""
            SELECT COUNT(r)
            FROM RechargeTransaction r
            WHERE r.tenant.id = :tenantId
              AND (:dateFrom IS NULL OR r.rechargeDate >= :dateFrom)
              AND (:dateTo   IS NULL OR r.rechargeDate <= :dateTo)
            """)
    long countByTenant(@Param("tenantId") Long tenantId,
                       @Param("dateFrom") LocalDate dateFrom,
                       @Param("dateTo") LocalDate dateTo);
}
