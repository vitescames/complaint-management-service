package com.complaintmanagementservice.domain.event;

import com.complaintmanagementservice.domain.model.ComplaintId;

import java.time.Instant;

public record ComplaintCreatedDomainEvent(ComplaintId complaintId, Instant occurredAt) implements DomainEvent {
}
