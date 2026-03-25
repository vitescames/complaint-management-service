package com.complaintmanagementservice.domain.model;

import com.complaintmanagementservice.domain.exception.DomainValidationException;

import java.text.Normalizer;
import java.util.Locale;

public final class CategoryKeyword {

    private final Long id;
    private final String value;
    private final String normalizedValue;

    public CategoryKeyword(Long id, String value) {
        if (id == null) {
            throw new DomainValidationException("O identificador da palavra-chave da categoria é obrigatório.");
        }
        if (value == null) {
            throw new DomainValidationException("A palavra-chave da categoria é obrigatória.");
        }

        String normalizedKeywordValue = value.trim();
        if (normalizedKeywordValue.isBlank()) {
            throw new DomainValidationException("A palavra-chave da categoria é obrigatória.");
        }

        this.id = id;
        this.value = normalizedKeywordValue;
        this.normalizedValue = normalize(normalizedKeywordValue);
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
