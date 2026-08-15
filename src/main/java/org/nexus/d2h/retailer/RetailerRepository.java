package org.nexus.d2h.retailer;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface RetailerRepository extends JpaRepository<Retailer, Long>, JpaSpecificationExecutor<Retailer> {

    boolean existsByTenantIdAndRetailerCode(Long tenantId, String retailerCode);

    boolean existsByTenantIdAndRetailerCodeAndIdNot(Long tenantId, String retailerCode, Long id);

    Optional<Retailer> findByIdAndTenantId(Long id, Long tenantId);

    Optional<Retailer> findByTenantIdAndRetailerCode(Long tenantId, String retailerCode);

    long countByTenantId(Long tenantId);

    long countByTenantIdAndStatus(Long tenantId, RetailerStatus status);
}
