package com.complaintmanagementservice.adapters.out.messaging;

import com.complaintmanagementservice.adapters.out.messaging.dto.ComplaintCreatedQueueMessage;
import com.complaintmanagementservice.adapters.out.messaging.dto.ComplaintSlaWarningQueueMessage;
import com.complaintmanagementservice.domain.event.ComplaintCreatedDomainEvent;
import com.complaintmanagementservice.domain.event.ComplaintSlaWarningTriggeredDomainEvent;
import org.springframework.stereotype.Component;

@Component
public class ComplaintMessagePayloadMapper {

    public ComplaintCreatedQueueMessage toComplaintCreatedMessage(ComplaintCreatedDomainEvent event) {
        return new ComplaintCreatedQueueMessage(event.complaintId().value(), event.occurredAt());
    }

    public ComplaintSlaWarningQueueMessage toSlaWarningMessage(ComplaintSlaWarningTriggeredDomainEvent event) {
        return new ComplaintSlaWarningQueueMessage(event.complaintId().value(), event.slaDeadlineDate());
    }
}
