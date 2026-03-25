package com.complaintmanagementservice.adapters.out.messaging;

import com.complaintmanagementservice.adapters.out.messaging.dto.ComplaintCreatedQueueMessage;
import com.complaintmanagementservice.adapters.out.messaging.dto.ComplaintSlaWarningQueueMessage;
import com.complaintmanagementservice.application.model.ComplaintCreatedNotification;
import com.complaintmanagementservice.application.model.ComplaintSlaWarningNotification;

public class ComplaintMessagePayloadMapper {

    public ComplaintCreatedQueueMessage toComplaintCreatedMessage(ComplaintCreatedNotification notification) {
        return new ComplaintCreatedQueueMessage(notification.complaintId().value(), notification.createdAt());
    }

    public ComplaintSlaWarningQueueMessage toSlaWarningMessage(ComplaintSlaWarningNotification notification) {
        return new ComplaintSlaWarningQueueMessage(notification.complaintId().value(), notification.slaDeadlineDate());
    }
}
