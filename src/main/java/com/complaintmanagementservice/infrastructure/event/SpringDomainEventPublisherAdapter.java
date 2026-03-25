package com.complaintmanagementservice.infrastructure.event;

import com.complaintmanagementservice.application.port.out.DomainEventPublisherPort;
import com.complaintmanagementservice.domain.event.DomainEvent;
import org.springframework.context.ApplicationEventPublisher;

public class SpringDomainEventPublisherAdapter implements DomainEventPublisherPort {

    private final ApplicationEventPublisher applicationEventPublisher;

    public SpringDomainEventPublisherAdapter(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Override
    public void publish(DomainEvent domainEvent) {
        applicationEventPublisher.publishEvent(domainEvent);
    }
}
