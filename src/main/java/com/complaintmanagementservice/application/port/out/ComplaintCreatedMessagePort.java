package com.complaintmanagementservice.application.port.out;

import com.complaintmanagementservice.application.model.ComplaintCreatedNotification;

public interface ComplaintCreatedMessagePort {

    void publish(ComplaintCreatedNotification notification);
}
