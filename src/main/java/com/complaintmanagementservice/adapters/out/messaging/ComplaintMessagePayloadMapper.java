package com.complaintmanagementservice.adapters.out.messaging;

import com.complaintmanagementservice.adapters.out.messaging.dto.ComplaintCreatedQueueMessage;
import com.complaintmanagementservice.adapters.out.messaging.dto.ComplaintSlaWarningQueueMessage;
import com.complaintmanagementservice.application.notification.ComplaintSlaWarningNotification;
import com.complaintmanagementservice.domain.event.ComplaintCreatedDomainEvent;
import org.springframework.stereotype.Component;

@Component
public class ComplaintMessagePayloadMapper {

    public ComplaintCreatedQueueMessage toComplaintCreatedMessage(ComplaintCreatedDomainEvent event) {
        return new ComplaintCreatedQueueMessage(event.complaintId().value(), event.occurredAt());
    }

    public ComplaintSlaWarningQueueMessage toSlaWarningMessage(ComplaintSlaWarningNotification notification) {
        return new ComplaintSlaWarningQueueMessage(notification.complaintId().value(), notification.slaDeadlineDate());
    }
}
