package com.kovan.app.exception;

public class SnsServiceException extends RuntimeException {
    public SnsServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}