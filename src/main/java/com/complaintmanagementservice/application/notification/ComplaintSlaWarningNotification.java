package com.complaintmanagementservice.application.notification;

import com.complaintmanagementservice.domain.model.ComplaintId;

import java.time.LocalDate;

public record ComplaintSlaWarningNotification(ComplaintId complaintId, LocalDate slaDeadlineDate) {
}
