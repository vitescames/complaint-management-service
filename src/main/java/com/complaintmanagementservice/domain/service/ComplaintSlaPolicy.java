package com.complaintmanagementservice.domain.service;

import com.complaintmanagementservice.domain.model.Complaint;
import com.complaintmanagementservice.domain.model.ComplaintStatus;

import java.time.LocalDate;

public class ComplaintSlaPolicy {

    private static final long SLA_DAYS = 10L;
    private static final long WARNING_OFFSET_DAYS = 3L;

    public LocalDate deadlineFor(Complaint complaint) {
        return complaint.complaintDate().plusDays(SLA_DAYS);
    }

    public LocalDate warningTriggerComplaintDate(LocalDate referenceDate) {
        return referenceDate.minusDays(SLA_DAYS - WARNING_OFFSET_DAYS);
    }

    public boolean isWarningDue(Complaint complaint, LocalDate referenceDate) {
        return complaint.status() != ComplaintStatus.RESOLVED
                && deadlineFor(complaint).minusDays(WARNING_OFFSET_DAYS).isEqual(referenceDate);
    }
}
