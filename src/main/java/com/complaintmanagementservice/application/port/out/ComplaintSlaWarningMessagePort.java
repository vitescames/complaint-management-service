package com.complaintmanagementservice.application.port.out;

import com.complaintmanagementservice.application.notification.ComplaintSlaWarningNotification;

public interface ComplaintSlaWarningMessagePort {

    void publish(ComplaintSlaWarningNotification notification);
}
