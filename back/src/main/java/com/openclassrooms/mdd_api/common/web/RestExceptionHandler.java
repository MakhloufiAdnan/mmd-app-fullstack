package com.openclassrooms.mdd_api.common.web;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestControllerAdvice
public class RestExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        List<FieldErrorItem> fieldErrors = ex.getBindingResult().getFieldErrors()
                .stream()
                .map(this::toFieldErrorItem)
                .toList();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                new ApiErrorResponse("VALIDATION_ERROR", "Validation error", fieldErrors)
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                new ApiErrorResponse("VALIDATION_ERROR", ex.getMessage(), List.of())
        );
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiErrorResponse> handleConflict(IllegalStateException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                new ApiErrorResponse("CONFLICT", ex.getMessage(), List.of())
        );
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleDbConflict(DataIntegrityViolationException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                new ApiErrorResponse("CONFLICT", "Conflict", List.of())
        );
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiErrorResponse> handleBadCredentials(BadCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                new ApiErrorResponse("UNAUTHORIZED", "Invalid credentials", List.of())
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGeneric(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                new ApiErrorResponse("INTERNAL", "Internal error", List.of())
        );
    }

    private FieldErrorItem toFieldErrorItem(FieldError fe) {
        return new FieldErrorItem(fe.getField(), fe.getDefaultMessage());
    }
}
