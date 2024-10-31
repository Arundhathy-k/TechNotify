package com.kovan.exception;

public class NewsRetrievalException extends RuntimeException {

    public NewsRetrievalException(String message) {
        super(message);
    }

    public NewsRetrievalException(String message, Throwable cause) {
        super(message, cause);
    }
}
