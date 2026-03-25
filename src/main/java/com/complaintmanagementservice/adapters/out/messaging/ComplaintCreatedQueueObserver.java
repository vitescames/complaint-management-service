package com.complaintmanagementservice.adapters.out.messaging;

import com.complaintmanagementservice.application.event.DomainEventObserver;
import com.complaintmanagementservice.application.port.out.ComplaintCreatedMessagePort;
import com.complaintmanagementservice.domain.event.ComplaintCreatedDomainEvent;
import org.springframework.stereotype.Component;

@Component
public class ComplaintCreatedQueueObserver implements DomainEventObserver<ComplaintCreatedDomainEvent> {

    private final ComplaintCreatedMessagePort complaintCreatedMessagePort;

    public ComplaintCreatedQueueObserver(ComplaintCreatedMessagePort complaintCreatedMessagePort) {
        this.complaintCreatedMessagePort = complaintCreatedMessagePort;
    }

    @Override
    public Class<ComplaintCreatedDomainEvent> supportedEventType() {
        return ComplaintCreatedDomainEvent.class;
    }

    @Override
    public void onEvent(ComplaintCreatedDomainEvent event) {
        complaintCreatedMessagePort.publish(event);
    }
}
