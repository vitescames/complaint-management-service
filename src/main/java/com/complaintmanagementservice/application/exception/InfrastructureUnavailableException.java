package com.complaintmanagementservice.application.exception;

public class InfrastructureUnavailableException extends RuntimeException {

    public InfrastructureUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
