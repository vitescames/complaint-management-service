package com.complaintmanagementservice.domain.model;

import com.complaintmanagementservice.domain.exception.DomainValidationException;

import java.net.URI;

public record DocumentUrl(String value) {

    public DocumentUrl {
        if (value == null) {
            throw new DomainValidationException("A URL do documento e obrigatoria");
        }
        String normalized = value.trim();
        try {
            URI uri = URI.create(normalized);
            if (uri.getScheme() == null || (!"http".equalsIgnoreCase(uri.getScheme()) && !"https".equalsIgnoreCase(uri.getScheme()))) {
                throw new DomainValidationException("A URL do documento deve ser HTTP ou HTTPS absoluta");
            }
        }
        catch (IllegalArgumentException exception) {
            throw new DomainValidationException("A URL do documento deve ser HTTP ou HTTPS absoluta");
        }
        value = normalized;
    }
}
