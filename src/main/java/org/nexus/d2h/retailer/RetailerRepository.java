package org.nexus.d2h.retailer;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface RetailerRepository extends JpaRepository<Retailer, Long>, JpaSpecificationExecutor<Retailer> {

    boolean existsByRetailerCode(String retailerCode);

    boolean existsByRetailerCodeAndIdNot(String retailerCode, Long id);

    Optional<Retailer> findByRetailerCode(String retailerCode);

    long countByStatus(RetailerStatus status);
}
