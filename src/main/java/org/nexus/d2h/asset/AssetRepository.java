package org.nexus.d2h.asset;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface AssetRepository extends JpaRepository<StbAsset, Long>, JpaSpecificationExecutor<StbAsset> {

    boolean existsByTenantIdAndSerialNumber(Long tenantId, String serialNumber);

    boolean existsByTenantIdAndSerialNumberAndIdNot(Long tenantId, String serialNumber, Long id);

    Optional<StbAsset> findByIdAndTenantId(Long id, Long tenantId);

    Optional<StbAsset> findByTenantIdAndSerialNumber(Long tenantId, String serialNumber);
}
