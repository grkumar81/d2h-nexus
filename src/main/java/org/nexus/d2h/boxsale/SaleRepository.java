package org.nexus.d2h.boxsale;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SaleRepository extends JpaRepository<StbSale, Long> {

    Optional<StbSale> findByIdAndTenantId(Long id, Long tenantId);

    Page<StbSale> findByTenantIdAndRetailerId(Long tenantId, Long retailerId, Pageable pageable);

    Page<StbSale> findByTenantId(Long tenantId, Pageable pageable);
}
