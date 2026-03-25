package com.complaintmanagementservice.domain.model;

import com.complaintmanagementservice.domain.exception.DomainValidationException;

public record CustomerName(String value) {

    public CustomerName {
        if (value == null) {
            throw new DomainValidationException("O nome do cliente é obrigatório.");
        }
        String normalized = value.trim();
        if (normalized.isBlank() || normalized.length() > 120) {
            throw new DomainValidationException("O nome do cliente deve ter entre 1 e 120 caracteres.");
        }
        value = normalized;
    }
}
