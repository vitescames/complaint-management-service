package com.complaintmanagementservice.domain.event;

import com.complaintmanagementservice.domain.model.ComplaintId;

import java.time.Instant;
import java.time.LocalDate;

public record ComplaintSlaWarningTriggeredDomainEvent(
        ComplaintId complaintId,
        LocalDate slaDeadlineDate,
        Instant occurredAt
) implements DomainEvent {
}
