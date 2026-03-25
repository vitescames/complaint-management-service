package com.complaintmanagementservice.application.model;

import com.complaintmanagementservice.domain.model.ComplaintId;

import java.time.Instant;
import java.util.Objects;

public record ComplaintCreatedNotification(ComplaintId complaintId, Instant createdAt) {

    public ComplaintCreatedNotification {
        Objects.requireNonNull(complaintId, "complaintId must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
    }
}
