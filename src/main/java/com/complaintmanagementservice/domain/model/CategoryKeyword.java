package com.complaintmanagementservice.domain.model;

import com.complaintmanagementservice.domain.exception.DomainValidationException;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Objects;

public final class CategoryKeyword {

    private final Long id;
    private final String value;
    private final String normalizedValue;

    public CategoryKeyword(Long id, String value) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.value = Objects.requireNonNull(value, "value must not be null").trim();
        if (this.value.isBlank()) {
            throw new DomainValidationException("Category keyword must not be blank");
        }
        this.normalizedValue = normalize(this.value);
    }

    public Long id() {
        return id;
    }

    public String value() {
        return value;
    }

    public boolean matches(String normalizedText) {
        return normalizedText.contains(normalizedValue);
    }

    private static String normalize(String text) {
        return Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
    }
}
