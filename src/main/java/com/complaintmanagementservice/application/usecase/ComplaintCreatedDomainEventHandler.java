package com.complaintmanagementservice.application.usecase;

import com.complaintmanagementservice.application.model.ComplaintCreatedNotification;
import com.complaintmanagementservice.application.port.out.ComplaintCreatedMessagePort;
import com.complaintmanagementservice.domain.event.ComplaintCreatedDomainEvent;

import java.util.Objects;

public class ComplaintCreatedDomainEventHandler {

    private final ComplaintCreatedMessagePort complaintCreatedMessagePort;

    public ComplaintCreatedDomainEventHandler(ComplaintCreatedMessagePort complaintCreatedMessagePort) {
        this.complaintCreatedMessagePort =
                Objects.requireNonNull(complaintCreatedMessagePort, "complaintCreatedMessagePort must not be null");
    }

    public void handle(ComplaintCreatedDomainEvent event) {
        complaintCreatedMessagePort.publish(new ComplaintCreatedNotification(event.complaintId(), event.occurredAt()));
    }
}
