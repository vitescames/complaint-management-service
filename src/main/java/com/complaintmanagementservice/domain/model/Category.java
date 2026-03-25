package com.complaintmanagementservice.domain.model;

import com.complaintmanagementservice.domain.exception.DomainValidationException;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

public final class Category {

    private final Long id;
    private final String name;
    private final Set<CategoryKeyword> keywords;

    public Category(Long id, String name, Set<CategoryKeyword> keywords) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.name = Objects.requireNonNull(name, "name must not be null").trim();
        if (this.name.isBlank()) {
            throw new DomainValidationException("Category name must not be blank");
        }
        this.keywords = Set.copyOf(new LinkedHashSet<>(Objects.requireNonNull(keywords, "keywords must not be null")));
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
}
