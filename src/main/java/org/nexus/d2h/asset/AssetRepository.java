package org.nexus.d2h.asset;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface AssetRepository extends JpaRepository<StbAsset, Long>, JpaSpecificationExecutor<StbAsset> {

    boolean existsBySerialNumber(String serialNumber);

    boolean existsBySerialNumberAndIdNot(String serialNumber, Long id);

    Optional<StbAsset> findBySerialNumber(String serialNumber);

    @Query("SELECT a.status, COUNT(a) FROM StbAsset a GROUP BY a.status")
    List<Object[]> countByStatus();

    long count();
}
