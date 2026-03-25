package com.complaintmanagementservice.domain.model;

import com.complaintmanagementservice.domain.exception.DomainValidationException;

import java.net.URI;
import java.util.Objects;

public record DocumentUrl(String value) {

    public DocumentUrl {
        Objects.requireNonNull(value, "value must not be null");
        String normalized = value.trim();
        try {
            URI uri = URI.create(normalized);
            if (uri.getScheme() == null || (!"http".equalsIgnoreCase(uri.getScheme()) && !"https".equalsIgnoreCase(uri.getScheme()))) {
                throw new DomainValidationException("Document URL must be an absolute HTTP or HTTPS URL");
            }
        }
        catch (IllegalArgumentException exception) {
            throw new DomainValidationException("Document URL must be an absolute HTTP or HTTPS URL");
        }
        value = normalized;
    }
}
