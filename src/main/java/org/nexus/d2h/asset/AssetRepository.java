package org.nexus.d2h.asset;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface AssetRepository extends JpaRepository<StbAsset, Long>, JpaSpecificationExecutor<StbAsset> {

    boolean existsByTenantIdAndSerialNumber(Long tenantId, String serialNumber);

    boolean existsByTenantIdAndSerialNumberAndIdNot(Long tenantId, String serialNumber, Long id);

    Optional<StbAsset> findByIdAndTenantId(Long id, Long tenantId);

    Optional<StbAsset> findByTenantIdAndSerialNumber(Long tenantId, String serialNumber);

    @org.springframework.data.jpa.repository.Query("""
            SELECT a.status, COUNT(a)
            FROM StbAsset a
            WHERE a.tenant.id = :tenantId
            GROUP BY a.status
            """)
    java.util.List<Object[]> countByStatusForTenant(@org.springframework.data.repository.query.Param("tenantId") Long tenantId);

    long countByTenantId(Long tenantId);
}
