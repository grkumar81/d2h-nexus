package org.nexus.d2h.asset;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssetHistoryRepository extends JpaRepository<StbAssetHistory, Long> {

    Page<StbAssetHistory> findByAssetIdAndTenantIdOrderByChangedAtDesc(Long assetId, Long tenantId, Pageable pageable);
}
