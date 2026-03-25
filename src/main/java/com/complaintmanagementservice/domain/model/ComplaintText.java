package com.complaintmanagementservice.domain.model;

import com.complaintmanagementservice.domain.exception.DomainValidationException;

import java.util.Objects;

public record ComplaintText(String value) {

    public ComplaintText {
        Objects.requireNonNull(value, "value must not be null");
        String normalized = value.trim();
        if (normalized.isBlank() || normalized.length() > 4000) {
            throw new DomainValidationException("Complaint text must contain between 1 and 4000 characters");
        }
        value = normalized;
    }
}
