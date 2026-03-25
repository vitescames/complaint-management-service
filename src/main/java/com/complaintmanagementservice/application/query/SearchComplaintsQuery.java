package com.complaintmanagementservice.application.query;

import com.complaintmanagementservice.application.exception.BusinessRuleViolationException;
import com.complaintmanagementservice.domain.model.ComplaintStatus;
import com.complaintmanagementservice.domain.model.Cpf;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public final class SearchComplaintsQuery {

    private final String customerCpf;
    private final List<String> categoryNames;
    private final List<Integer> statusIds;
    private final LocalDate startDate;
    private final LocalDate endDate;

    private SearchComplaintsQuery(Builder builder) {
        this.customerCpf = builder.customerCpf;
        this.categoryNames = List.copyOf(builder.categoryNames);
        this.statusIds = List.copyOf(builder.statusIds);
        this.startDate = builder.startDate;
        this.endDate = builder.endDate;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String customerCpf() {
        return customerCpf;
    }

    public List<String> categoryNames() {
        return categoryNames;
    }

    public List<Integer> statusIds() {
        return statusIds;
    }

    public LocalDate startDate() {
        return startDate;
    }

    public LocalDate endDate() {
        return endDate;
    }

    public boolean hasCustomerCpf() {
        return customerCpf != null;
    }

    public boolean hasStartDate() {
        return startDate != null;
    }

    public boolean hasEndDate() {
        return endDate != null;
    }

    public static final class Builder {

        private String customerCpf;
        private List<String> categoryNames = List.of();
        private List<Integer> statusIds = List.of();
        private LocalDate startDate;
        private LocalDate endDate;

        private Builder() {
        }

        public Builder customerCpf(String customerCpf) {
            this.customerCpf = customerCpf;
            return this;
        }

        public Builder categoryNames(List<String> categoryNames) {
            this.categoryNames = categoryNames == null ? List.of() : new ArrayList<>(categoryNames);
            return this;
        }

        public Builder statusIds(List<Integer> statusIds) {
            this.statusIds = statusIds == null ? List.of() : new ArrayList<>(statusIds);
            return this;
        }

        public Builder startDate(LocalDate startDate) {
            this.startDate = startDate;
            return this;
        }

        public Builder endDate(LocalDate endDate) {
            this.endDate = endDate;
            return this;
        }

        public SearchComplaintsQuery build() {
            customerCpf = normalize(customerCpf);
            if (customerCpf != null) {
                new Cpf(customerCpf);
            }

            List<String> normalizedCategories = new ArrayList<>();
            for (String categoryName : categoryNames) {
                String normalized = normalize(categoryName);
                if (normalized != null) {
                    normalizedCategories.add(normalized);
                }
            }
            categoryNames = normalizedCategories;

            List<Integer> validatedStatusIds = new ArrayList<>();
            for (Integer statusId : statusIds) {
                if (statusId != null) {
                    ComplaintStatus.fromId(statusId);
                    validatedStatusIds.add(statusId);
                }
            }
            statusIds = validatedStatusIds;

            if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
                throw new BusinessRuleViolationException("A data inicial deve ser menor ou igual a data final");
            }

            return new SearchComplaintsQuery(this);
        }

        private String normalize(String value) {
            if (value == null) {
                return null;
            }
            String normalized = value.trim();
            return normalized.isEmpty() ? null : normalized;
        }
    }
}
