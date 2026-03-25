package com.complaintmanagementservice.application.port.out;

import com.complaintmanagementservice.application.model.ComplaintSlaWarningNotification;

public interface ComplaintSlaWarningMessagePort {

    void publish(ComplaintSlaWarningNotification notification);
}
