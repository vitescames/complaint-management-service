package com.complaintmanagementservice.domain.model;

import com.complaintmanagementservice.domain.exception.DomainValidationException;

import java.net.URI;

public record DocumentUrl(String value) {

    public DocumentUrl {
        if (value == null) {
            throw new DomainValidationException("A URL do documento é obrigatória.");
        }

        String normalizedValue = value.trim();
        URI uri = toUri(normalizedValue);
        if (!isSupportedAbsoluteHttpUrl(uri)) {
            throw new DomainValidationException("A URL do documento deve ser HTTP ou HTTPS absoluta.");
        }

        value = normalizedValue;
    }

    private static URI toUri(String value) {
        try {
            return URI.create(value);
        }
        catch (IllegalArgumentException exception) {
            throw new DomainValidationException("A URL do documento deve ser HTTP ou HTTPS absoluta.");
        }
    }

    private static boolean isSupportedAbsoluteHttpUrl(URI uri) {
        String scheme = uri.getScheme();
        return uri.isAbsolute()
                && uri.getHost() != null
                && ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme));
    }
}
