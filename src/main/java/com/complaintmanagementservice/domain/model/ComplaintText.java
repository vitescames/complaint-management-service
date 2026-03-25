package com.complaintmanagementservice.domain.model;

import com.complaintmanagementservice.domain.exception.DomainValidationException;

public record ComplaintText(String value) {

    public ComplaintText {
        if (value == null) {
            throw new DomainValidationException("O texto da reclamacao e obrigatorio");
        }
        String normalized = value.trim();
        if (normalized.isBlank() || normalized.length() > 4000) {
            throw new DomainValidationException("O texto da reclamacao deve ter entre 1 e 4000 caracteres");
        }
        value = normalized;
    }
}
