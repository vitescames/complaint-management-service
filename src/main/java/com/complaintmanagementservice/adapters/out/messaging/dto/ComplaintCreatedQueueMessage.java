package com.complaintmanagementservice.adapters.out.messaging.dto;

import java.time.Instant;
import java.util.UUID;

public record ComplaintCreatedQueueMessage(UUID complaintId, Instant createdAt) {
}
