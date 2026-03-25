package com.complaintmanagementservice.application.port.out;

import com.complaintmanagementservice.domain.event.ComplaintCreatedDomainEvent;

public interface ComplaintCreatedMessagePort {

    void publish(ComplaintCreatedDomainEvent event);
}
