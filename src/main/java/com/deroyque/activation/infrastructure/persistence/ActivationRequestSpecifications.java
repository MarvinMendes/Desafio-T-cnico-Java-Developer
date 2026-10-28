package com.deroyque.activation.infrastructure.persistence;

import com.deroyque.activation.domain.ActivationRequest;
import com.deroyque.activation.domain.ActivationStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public final class ActivationRequestSpecifications {

    private ActivationRequestSpecifications() {
    }

    public static Specification<ActivationRequest> withFilters(
            String ownerClientId,
            ActivationStatus status,
            String customerId
    ) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(builder.equal(root.get("ownerClientId"), ownerClientId));
            if (status != null) {
                predicates.add(builder.equal(root.get("status"), status));
            }
            if (customerId != null && !customerId.isBlank()) {
                predicates.add(builder.equal(root.get("customerId"), customerId));
            }
            return builder.and(predicates.toArray(Predicate[]::new));
        };
    }
}
