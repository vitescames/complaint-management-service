package com.complaintmanagementservice.infrastructure.event;

import com.complaintmanagementservice.application.usecase.ComplaintCreatedDomainEventHandler;
import com.complaintmanagementservice.domain.event.ComplaintCreatedDomainEvent;
import org.springframework.context.event.EventListener;

public class SpringComplaintCreatedEventObserver {

    private final ComplaintCreatedDomainEventHandler complaintCreatedDomainEventHandler;

    public SpringComplaintCreatedEventObserver(ComplaintCreatedDomainEventHandler complaintCreatedDomainEventHandler) {
        this.complaintCreatedDomainEventHandler = complaintCreatedDomainEventHandler;
    }

    @EventListener
    public void onComplaintCreated(ComplaintCreatedDomainEvent event) {
        complaintCreatedDomainEventHandler.handle(event);
    }
}
