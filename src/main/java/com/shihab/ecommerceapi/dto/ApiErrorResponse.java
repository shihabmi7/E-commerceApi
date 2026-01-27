package com.shihab.ecommerceapi.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class ApiErrorResponse {
    private int status;
    private String message;
    private LocalDateTime timeStamp;

    public ApiErrorResponse(int status, String message) {
        this.status = status;
        this.message = message;
        this.timeStamp = LocalDateTime.now();
    }

}
