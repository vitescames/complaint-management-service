package com.complaintmanagementservice.domain.model;

import com.complaintmanagementservice.domain.exception.DomainValidationException;

import java.util.Objects;

public record CustomerName(String value) {

    public CustomerName {
        Objects.requireNonNull(value, "value must not be null");
        String normalized = value.trim();
        if (normalized.isBlank() || normalized.length() > 120) {
            throw new DomainValidationException("Customer name must contain between 1 and 120 characters");
        }
        value = normalized;
    }
}
