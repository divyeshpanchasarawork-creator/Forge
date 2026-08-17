package com.forge.common.exception;

import com.forge.common.dto.ApiResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.apache.catalina.connector.ClientAbortException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    void resourceNotFoundMapsTo404() {
        ResponseEntity<ApiResponse<Void>> result =
                handler.handleResourceNotFound(new ResourceNotFoundException("Problem", "id", UUID.randomUUID()));

        assertEquals(HttpStatus.NOT_FOUND, result.getStatusCode());
        assertFalse(result.getBody().isSuccess());
        assertTrue(result.getBody().getMessage().contains("Problem"));
    }

    @Test
    void badRequestMapsTo400() {
        ResponseEntity<ApiResponse<Void>> result = handler.handleBadRequest(new BadRequestException("nope"));

        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
        assertEquals("nope", result.getBody().getMessage());
    }

    @Test
    void serviceUnavailableMapsTo502() {
        ResponseEntity<ApiResponse<Void>> result =
                handler.handleServiceUnavailable(new ServiceUnavailableException("upstream down"));

        assertEquals(HttpStatus.BAD_GATEWAY, result.getStatusCode());
    }

    @Test
    void methodArgumentValidationMapsTo400WithFieldErrors() {
        BeanPropertyBindingResult binding = new BeanPropertyBindingResult(new Object(), "payload");
        binding.addError(new FieldError("payload", "title", "must not be blank"));

        ResponseEntity<ApiResponse<Map<String, String>>> result =
                handler.handleValidation(new MethodArgumentNotValidException(null, binding));

        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
        assertFalse(result.getBody().isSuccess());
        assertEquals("must not be blank", result.getBody().getData().get("title"));
    }

    @Test
    void constraintViolationMapsTo400() {
        ConstraintViolationException ex = new ConstraintViolationException(
                "invalid", Collections.<ConstraintViolation<?>>emptySet());

        ResponseEntity<ApiResponse<Void>> result = handler.handleMethodValidation(ex);

        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
        assertEquals("Invalid request parameter", result.getBody().getMessage());
    }

    @Test
    void typeMismatchAndMissingParameterMapTo400() {
        ResponseEntity<ApiResponse<Void>> mismatch =
                handler.handleInvalidParameter(new MethodArgumentTypeMismatchException("abc", Integer.class, "days", null, null));
        ResponseEntity<ApiResponse<Void>> missing =
                handler.handleInvalidParameter(new MissingServletRequestParameterException("page", "int"));

        assertEquals(HttpStatus.BAD_REQUEST, mismatch.getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, missing.getStatusCode());
    }

    @Test
    void authenticationFailureMapsTo401() {
        ResponseEntity<ApiResponse<Void>> result = handler.handleAuthentication(new BadCredentialsException("bad"));

        assertEquals(HttpStatus.UNAUTHORIZED, result.getStatusCode());
        assertEquals("Invalid username or password", result.getBody().getMessage());
    }

    @Test
    void dataIntegrityMapsTo409() {
        ResponseEntity<ApiResponse<Void>> result = handler.handleDataIntegrity(new DataIntegrityViolationException("dup"));

        assertEquals(HttpStatus.CONFLICT, result.getStatusCode());
    }

    @Test
    void optimisticLockMapsTo409() {
        ResponseEntity<ApiResponse<Void>> result =
                handler.handleOptimisticLock(new ObjectOptimisticLockingFailureException("stale", new Object()));

        assertEquals(HttpStatus.CONFLICT, result.getStatusCode());
        assertTrue(result.getBody().getMessage().contains("retry"));
    }

    @Test
    void unreadableBodyMapsTo400() {
        ResponseEntity<ApiResponse<Void>> result = handler.handleUnreadable(new HttpMessageNotReadableException("bad json", (org.springframework.http.HttpInputMessage) null));

        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
        assertEquals("Malformed request body", result.getBody().getMessage());
    }

    @Test
    void genericExceptionMapsTo500() {
        ResponseEntity<ApiResponse<Void>> result = handler.handleGeneral(new IllegalStateException("boom"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, result.getStatusCode());
        assertFalse(result.getBody().isSuccess());
    }

    @Test
    void clientAbortIsSwallowed() {
        assertDoesNotThrow(() -> handler.handleClientAbort(new ClientAbortException("gone")));
    }
}
