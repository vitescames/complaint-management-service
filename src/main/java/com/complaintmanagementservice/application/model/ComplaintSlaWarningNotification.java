package com.complaintmanagementservice.application.model;

import com.complaintmanagementservice.domain.model.ComplaintId;

import java.time.LocalDate;
import java.util.Objects;

public record ComplaintSlaWarningNotification(ComplaintId complaintId, LocalDate slaDeadlineDate) {

    public ComplaintSlaWarningNotification {
        Objects.requireNonNull(complaintId, "complaintId must not be null");
        Objects.requireNonNull(slaDeadlineDate, "slaDeadlineDate must not be null");
    }
}
