package com.complaintmanagementservice.application.exception;

public class ApplicationValidationException extends RuntimeException {

    public ApplicationValidationException(String message) {
        super(message);
    }
}
