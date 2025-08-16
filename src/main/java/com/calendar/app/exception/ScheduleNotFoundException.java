package com.calendar.app.exception;

public class ScheduleNotFoundException extends RuntimeException {
    
    public ScheduleNotFoundException(String message) {
        super(message);
    }
    
    public ScheduleNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
