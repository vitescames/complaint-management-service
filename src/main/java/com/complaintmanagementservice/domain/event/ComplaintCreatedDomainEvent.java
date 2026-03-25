package com.complaintmanagementservice.domain.event;

import com.complaintmanagementservice.domain.model.ComplaintId;

import java.time.Instant;

public record ComplaintCreatedDomainEvent(ComplaintId complaintId, Instant occurredAt) implements DomainEvent {

    public ComplaintCreatedDomainEvent {
        if (complaintId == null) {
            throw new IllegalArgumentException("complaintId must not be null");
        }
        if (occurredAt == null) {
            throw new IllegalArgumentException("occurredAt must not be null");
        }
    }
}
