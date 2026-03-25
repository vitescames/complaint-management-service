package com.complaintmanagementservice.application.port.out;

import com.complaintmanagementservice.domain.event.DomainEvent;

public interface DomainEventPublisherPort {

    void publish(DomainEvent domainEvent);
}
