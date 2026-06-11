package com.shihab.ecommerceapi.dto;

import java.time.LocalDateTime;
import java.util.List;

public class ValidationErrorResponse {
    private int status;
    private String message;
    private List<String> errors;
    private LocalDateTime timeStamp;

    public ValidationErrorResponse(int status, String message, List<String> errors) {
        this.status = status;
        this.message = message;
        this.errors = errors;
        this.timeStamp = LocalDateTime.now();
    }

    public int getStatus() { return status; }
    public String getMessage() { return message; }
    public List<String> getErrors() { return errors; }
    public LocalDateTime getTimeStamp() { return timeStamp; }
}
