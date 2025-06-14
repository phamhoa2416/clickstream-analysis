package com.example.clickstream.exception;

public class ProducerException extends RuntimeException {
    public ProducerException(String message) {
        super(message);
    }

    public ProducerException(String message, Throwable cause) {
        super(message, cause);
    }
}
