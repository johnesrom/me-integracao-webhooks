package com.exemplo.mewebhooks.exception;

public class InvalidWebhookPayloadException extends RuntimeException {
    public InvalidWebhookPayloadException(String message) {
        super(message);
    }
}
