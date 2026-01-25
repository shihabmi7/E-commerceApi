package com.shihab.ecommerceapi.controller;

import com.shihab.ecommerceapi.dto.ApiErrorResponse;
import com.shihab.ecommerceapi.exception.EntityNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ApiErrorResponse>
    handleUserNotFoundException(EntityNotFoundException exception) {

        return new ResponseEntity<>(new ApiErrorResponse
                (HttpStatus.NOT_FOUND.value(), exception.getMessage()), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler
    public ResponseEntity<ApiErrorResponse> handleGenericException(Exception e) {
        return new ResponseEntity<>(new ApiErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Something went wrong. Please contact support."), HttpStatus.INTERNAL_SERVER_ERROR);

    }

}
