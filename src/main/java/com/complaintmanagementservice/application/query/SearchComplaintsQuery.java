package com.complaintmanagementservice.application.query;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class SearchComplaintsQuery {

    private final String customerCpf;
    private final List<String> categoryNames;
    private final List<Integer> statusIds;
    private final LocalDate startDate;
    private final LocalDate endDate;

    private SearchComplaintsQuery(Builder builder) {
        this.customerCpf = builder.customerCpf;
        this.categoryNames = Collections.unmodifiableList(new ArrayList<>(builder.categoryNames));
        this.statusIds = Collections.unmodifiableList(new ArrayList<>(builder.statusIds));
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
            return new SearchComplaintsQuery(this);
        }
    }
}
