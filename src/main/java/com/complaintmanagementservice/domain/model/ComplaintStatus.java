package com.complaintmanagementservice.domain.model;

import com.complaintmanagementservice.domain.exception.DomainValidationException;

import java.util.Arrays;

public enum ComplaintStatus {
    PENDING(1),
    PROCESSING(2),
    RESOLVED(3);

    private final int id;

    ComplaintStatus(int id) {
        this.id = id;
    }

    public int id() {
        return id;
    }

    public static ComplaintStatus fromId(int id) {
        return Arrays.stream(values())
                .filter(status -> status.id == id)
                .findFirst()
                .orElseThrow(() -> new DomainValidationException("Complaint status id must be valid"));
    }
}
