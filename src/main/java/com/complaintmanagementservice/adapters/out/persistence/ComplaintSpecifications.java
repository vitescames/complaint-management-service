package com.complaintmanagementservice.adapters.out.persistence;

import com.complaintmanagementservice.adapters.out.persistence.entity.ComplaintEntity;
import com.complaintmanagementservice.application.query.SearchComplaintsQuery;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public final class ComplaintSpecifications {

    private ComplaintSpecifications() {
    }

    public static Specification<ComplaintEntity> from(SearchComplaintsQuery query) {
        return (root, criteriaQuery, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (query.hasCustomerCpf()) {
                predicates.add(criteriaBuilder.equal(root.get("customer").get("cpf"), query.customerCpf()));
            }
            if (!query.categoryNames().isEmpty()) {
                predicates.add(root.join("categories").get("name").in(query.categoryNames()));
                criteriaQuery.distinct(true);
            }
            if (!query.statusIds().isEmpty()) {
                predicates.add(root.get("status").get("id").in(query.statusIds()));
            }
            if (query.hasStartDate()) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("complaintDate"), query.startDate()));
            }
            if (query.hasEndDate()) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("complaintDate"), query.endDate()));
            }

            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }
}
