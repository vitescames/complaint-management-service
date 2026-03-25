package com.complaintmanagementservice.domain.model;

import com.complaintmanagementservice.domain.exception.DomainValidationException;

import java.util.LinkedHashSet;
import java.util.Set;

public final class Category {

    private final Long id;
    private final String name;
    private final Set<CategoryKeyword> keywords;

    private Category(Builder builder) {
        if (builder.id == null) {
            throw new DomainValidationException("O identificador da categoria é obrigatório.");
        }
        if (builder.name == null) {
            throw new DomainValidationException("O nome da categoria é obrigatório.");
        }

        String normalizedName = builder.name.trim();
        if (normalizedName.isBlank()) {
            throw new DomainValidationException("O nome da categoria é obrigatório.");
        }

        this.id = builder.id;
        this.name = normalizedName;
        this.keywords = builder.keywords == null ? Set.of() : Set.copyOf(new LinkedHashSet<>(builder.keywords));
    }

    public static Builder builder() {
        return new Builder();
    }

    public Long id() {
        return id;
    }

    public String name() {
        return name;
    }

    public Set<CategoryKeyword> keywords() {
        return keywords;
    }

    public static final class Builder {

        private Long id;
        private String name;
        private Set<CategoryKeyword> keywords;

        private Builder() {
        }

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder keywords(Set<CategoryKeyword> keywords) {
            this.keywords = keywords;
            return this;
        }

        public Category build() {
            return new Category(this);
        }
    }
}
