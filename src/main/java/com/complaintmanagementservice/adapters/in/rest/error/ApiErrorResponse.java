package com.complaintmanagementservice.adapters.in.rest.error;

public record ApiErrorResponse(String title, int status, String message) {
}
