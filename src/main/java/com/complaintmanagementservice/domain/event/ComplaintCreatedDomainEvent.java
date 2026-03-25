package com.complaintmanagementservice.domain.event;

import com.complaintmanagementservice.domain.model.ComplaintId;

import java.time.Instant;
import java.util.Objects;

public record ComplaintCreatedDomainEvent(ComplaintId complaintId, Instant occurredAt) implements DomainEvent {

    public ComplaintCreatedDomainEvent {
        Objects.requireNonNull(complaintId, "complaintId must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
    }
}
