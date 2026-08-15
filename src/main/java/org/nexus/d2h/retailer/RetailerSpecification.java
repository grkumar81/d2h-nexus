package org.nexus.d2h.retailer;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public final class RetailerSpecification {

    private RetailerSpecification() {}

    public static Specification<Retailer> search(Long tenantId, String query, RetailerStatus status) {
        return (root, cq, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Tenant isolation — always applied
            predicates.add(cb.equal(root.get("tenant").get("id"), tenantId));

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            if (query != null && !query.isBlank()) {
                String like = "%" + query.toLowerCase() + "%";
                predicates.add(cb.or(
                    cb.like(cb.lower(root.get("retailerCode")), like),
                    cb.like(cb.lower(root.get("retailerName")), like),
                    cb.like(cb.lower(root.get("mobile")), like),
                    cb.like(cb.lower(root.get("city")), like),
                    cb.like(cb.lower(root.get("state")), like)
                ));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
