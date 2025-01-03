package com.kovan.app.exception;

public class SqsServiceException extends RuntimeException {

    public SqsServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}

