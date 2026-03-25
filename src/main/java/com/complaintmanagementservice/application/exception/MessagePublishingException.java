package com.complaintmanagementservice.application.exception;

public class MessagePublishingException extends RuntimeException {

    public MessagePublishingException(String message, Throwable cause) {
        super(message, cause);
    }
}
