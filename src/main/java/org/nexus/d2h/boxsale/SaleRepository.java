package org.nexus.d2h.boxsale;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SaleRepository extends JpaRepository<StbSale, Long> {

    Page<StbSale> findByRetailerId(Long retailerId, Pageable pageable);
}
