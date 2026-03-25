package com.complaintmanagementservice.domain.event;

import java.time.Instant;

public interface DomainEvent {

    Instant occurredAt();
}
