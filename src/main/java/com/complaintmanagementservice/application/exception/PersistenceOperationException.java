package com.complaintmanagementservice.application.exception;

public class PersistenceOperationException extends RuntimeException {

    public PersistenceOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}
