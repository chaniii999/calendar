package com.calendar.app.exception;

public class InvalidCompletionRateException extends RuntimeException {
    
    public InvalidCompletionRateException(String message) {
        super(message);
    }
    
    public InvalidCompletionRateException(String message, Throwable cause) {
        super(message, cause);
    }
}
