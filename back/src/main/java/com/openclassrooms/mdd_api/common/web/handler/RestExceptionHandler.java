package com.openclassrooms.mdd_api.common.web.handler;

import com.openclassrooms.mdd_api.common.web.response.ApiErrorCodes;
import com.openclassrooms.mdd_api.common.web.response.ApiErrorResponse;
import com.openclassrooms.mdd_api.common.web.response.FieldErrorItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestControllerAdvice
public class RestExceptionHandler {

    private ResponseEntity<ApiErrorResponse> createErrorResponse(String code, String message, List<FieldErrorItem> fieldErrors) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                new ApiErrorResponse(code, message, fieldErrors)
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        List<FieldErrorItem> fieldErrors = ex.getBindingResult().getFieldErrors()
                .stream()
                .map(this::toFieldErrorItem)
                .toList();

        return createErrorResponse(ApiErrorCodes.VALIDATION_ERROR, "Validation error", fieldErrors);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleNotReadable(HttpMessageNotReadableException ex) {
        return createErrorResponse(ApiErrorCodes.VALIDATION_ERROR, "Malformed or missing JSON body", List.of());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleBadRequest(IllegalArgumentException ex) {
        return createErrorResponse(ApiErrorCodes.VALIDATION_ERROR, ex.getMessage(), List.of());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiErrorResponse> handleConflict(IllegalStateException ex) {
        return createErrorResponse(ApiErrorCodes.CONFLICT, ex.getMessage(), List.of());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleDbConflict(DataIntegrityViolationException ex) {
        return createErrorResponse(ApiErrorCodes.CONFLICT, "Conflict", List.of());
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiErrorResponse> handleBadCredentials(BadCredentialsException ex) {
        return createErrorResponse(ApiErrorCodes.UNAUTHORIZED, "Invalid credentials", List.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGeneric(Exception ex) {
        log.error("Unhandled exception", ex);
        return createErrorResponse(ApiErrorCodes.INTERNAL, "Internal error", List.of());
    }

    private FieldErrorItem toFieldErrorItem(FieldError fe) {
        return new FieldErrorItem(fe.getField(), fe.getDefaultMessage());
    }
}
