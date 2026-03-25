package com.complaintmanagementservice.application.event;

import com.complaintmanagementservice.domain.event.DomainEvent;

public interface DomainEventPublisher {

    void register(DomainEventObserver<? extends DomainEvent> observer);

    void remove(DomainEventObserver<? extends DomainEvent> observer);

    void publish(DomainEvent event);
}
