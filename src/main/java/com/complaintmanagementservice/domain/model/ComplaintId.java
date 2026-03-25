package com.complaintmanagementservice.domain.model;

import com.complaintmanagementservice.domain.exception.DomainValidationException;

import java.util.Objects;
import java.util.UUID;

public record ComplaintId(UUID value) {

    public ComplaintId {
        Objects.requireNonNull(value, "value must not be null");
    }

    public static ComplaintId newId() {
        return new ComplaintId(UUID.randomUUID());
    }

    public static ComplaintId from(String rawValue) {
        try {
            return new ComplaintId(UUID.fromString(rawValue));
        }
        catch (IllegalArgumentException exception) {
            throw new DomainValidationException("Complaint id must be a valid UUID");
        }
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
