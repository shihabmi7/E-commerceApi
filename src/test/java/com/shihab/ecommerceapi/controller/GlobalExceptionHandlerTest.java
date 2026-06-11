package com.shihab.ecommerceapi.controller;

import com.shihab.ecommerceapi.exception.BadRequestException;
import com.shihab.ecommerceapi.exception.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    void handleEntityNotFoundException_returns404() {
        var response = handler.handleEntityNotFoundException(new EntityNotFoundException("not found"));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().getMessage()).isEqualTo("not found");
    }

    @Test
    void handleBadRequestException_returns400() {
        var response = handler.handleBadRequestException(new BadRequestException("bad input"));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getMessage()).isEqualTo("bad input");
    }

    @Test
    void handleAccessDeniedException_returns403() {
        var response = handler.handleAccessDeniedException(new AccessDeniedException("denied"));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void handleBadCredentialsException_returns401() {
        var response = handler.handleBadCredentialsException(new BadCredentialsException("invalid creds"));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void handleDisabledException_returns401() {
        var response = handler.handleDisabledException(new DisabledException("disabled"));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void handleLockedException_returns401() {
        var response = handler.handleLockedException(new LockedException("locked"));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void handleMethodNotSupportedException_returns405() {
        var ex = new HttpRequestMethodNotSupportedException("DELETE");
        var response = handler.handleMethodNotSupportedException(ex);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
        assertThat(response.getBody().getMessage()).contains("DELETE");
    }

    @Test
    void handleGenericException_returns500() {
        var response = handler.handleGenericException(new RuntimeException("unexpected"));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void handleMissingParamException_returns400WithParamName() throws Exception {
        var ex = new MissingServletRequestParameterException("userId", "Integer");
        var response = handler.handleMissingParamException(ex);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getMessage()).contains("userId");
    }
}
