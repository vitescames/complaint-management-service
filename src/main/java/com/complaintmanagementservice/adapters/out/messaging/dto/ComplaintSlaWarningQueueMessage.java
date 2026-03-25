package com.complaintmanagementservice.adapters.out.messaging.dto;

import java.time.LocalDate;
import java.util.UUID;

public record ComplaintSlaWarningQueueMessage(UUID complaintId, LocalDate slaDeadlineDate) {
}
