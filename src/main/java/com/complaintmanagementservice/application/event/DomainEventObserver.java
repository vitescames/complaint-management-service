package com.complaintmanagementservice.application.event;

import com.complaintmanagementservice.domain.event.DomainEvent;

public interface DomainEventObserver<T extends DomainEvent> {

    Class<T> supportedEventType();

    void onEvent(T event);

    default boolean supports(DomainEvent event) {
        return supportedEventType().isInstance(event);
    }
}
