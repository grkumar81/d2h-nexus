package org.nexus.d2h.recharge;

import jakarta.persistence.criteria.Predicate;
import org.nexus.d2h.finance.PaymentMethod;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public final class RechargeSpecification {

    private RechargeSpecification() {}

    public static Specification<RechargeTransaction> search(
            Long retailerId,
            RechargeType type,
            RechargeStatus status,
            PaymentMethod paymentMethod,
            LocalDate dateFrom,
            LocalDate dateTo,
            String reference,
            BigDecimal amountMin,
            BigDecimal amountMax) {

        return (root, cq, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (retailerId != null)
                predicates.add(cb.equal(root.get("retailer").get("id"), retailerId));
            if (type != null)
                predicates.add(cb.equal(root.get("rechargeType"), type));
            if (status != null)
                predicates.add(cb.equal(root.get("rechargeStatus"), status));
            if (paymentMethod != null)
                predicates.add(cb.equal(root.get("paymentMethod"), paymentMethod));
            if (dateFrom != null)
                predicates.add(cb.greaterThanOrEqualTo(root.get("rechargeDate"), dateFrom));
            if (dateTo != null)
                predicates.add(cb.lessThanOrEqualTo(root.get("rechargeDate"), dateTo));
            if (reference != null && !reference.isBlank())
                predicates.add(cb.like(cb.lower(root.get("reference")),
                        "%" + reference.trim().toLowerCase() + "%"));
            if (amountMin != null)
                predicates.add(cb.greaterThanOrEqualTo(root.get("amount"), amountMin));
            if (amountMax != null)
                predicates.add(cb.lessThanOrEqualTo(root.get("amount"), amountMax));

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
