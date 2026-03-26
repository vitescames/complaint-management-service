package com.complaintmanagementservice.domain.model;

import com.complaintmanagementservice.domain.exception.DomainValidationException;

import java.util.UUID;

public record ComplaintId(UUID value) {

    public ComplaintId {
        if (value == null) {
            throw new DomainValidationException("O identificador da reclamação é obrigatório.");
        }
    }

    public static ComplaintId newId() {
        return new ComplaintId(UUID.randomUUID());
    }

    public static ComplaintId from(String rawValue) {
        try {
            return new ComplaintId(UUID.fromString(rawValue));
        }
        catch (IllegalArgumentException exception) {
            throw new DomainValidationException("O identificador da reclamação é inválido.");
        }
    }
}
