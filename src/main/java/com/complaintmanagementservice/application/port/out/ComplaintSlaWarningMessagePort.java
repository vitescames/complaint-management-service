package com.complaintmanagementservice.application.port.out;

import com.complaintmanagementservice.domain.event.ComplaintSlaWarningTriggeredDomainEvent;

public interface ComplaintSlaWarningMessagePort {

    void publish(ComplaintSlaWarningTriggeredDomainEvent event);
}
