package com.openclassrooms.mdd_api.common.web.handler;

import com.openclassrooms.mdd_api.common.web.exception.ApiBadRequestException;
import com.openclassrooms.mdd_api.common.web.exception.ApiConflictException;
import com.openclassrooms.mdd_api.common.web.exception.ApiNotFoundException;
import com.openclassrooms.mdd_api.common.web.exception.ApiUnauthorizedException;
import com.openclassrooms.mdd_api.common.web.response.ApiErrorCodes;
import com.openclassrooms.mdd_api.common.web.response.ApiErrorResponse;
import com.openclassrooms.mdd_api.common.web.response.FieldErrorItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

/**
 * Centralized API error mapping.
 * Contract: always returns {error,message,fieldErrors} and uses correct HTTP statuses
 * (400/401/403/409/500).
 */
@Slf4j
@RestControllerAdvice
public class RestExceptionHandler {

    private ResponseEntity<ApiErrorResponse> createErrorResponse(
            HttpStatus status,
            String code,
            String message,
            List<FieldErrorItem> fieldErrors
    ) {
        List<FieldErrorItem> safeFieldErrors = fieldErrors == null ? List.of() : List.copyOf(fieldErrors);
        return ResponseEntity.status(status).body(new ApiErrorResponse(code, message, safeFieldErrors));
    }

    // 400 - Validation

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        List<FieldErrorItem> fieldErrors = ex.getBindingResult().getFieldErrors()
                .stream()
                .map(this::toFieldErrorItem)
                .toList();

        return createErrorResponse(
                HttpStatus.BAD_REQUEST,
                ApiErrorCodes.VALIDATION_ERROR,
                "Validation error",
                fieldErrors
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleUnreadable(HttpMessageNotReadableException ex) {
        return createErrorResponse(
                HttpStatus.BAD_REQUEST,
                ApiErrorCodes.VALIDATION_ERROR,
                "Malformed request body",
                List.of()
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleBadRequest(IllegalArgumentException ex) {
        return createErrorResponse(
                HttpStatus.BAD_REQUEST,
                ApiErrorCodes.VALIDATION_ERROR,
                ex.getMessage(),
                List.of()
        );
    }

    @ExceptionHandler(ApiBadRequestException.class)
    public ResponseEntity<ApiErrorResponse> handleApiBadRequest(ApiBadRequestException ex) {
        return createErrorResponse(
                HttpStatus.BAD_REQUEST,
                ApiErrorCodes.VALIDATION_ERROR,
                ex.getMessage(),
                ex.getFieldErrors()
        );
    }

    // 401 - Unauthorized

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiErrorResponse> handleBadCredentials(BadCredentialsException ex) {
        return createErrorResponse(
                HttpStatus.UNAUTHORIZED,
                ApiErrorCodes.UNAUTHORIZED,
                "Invalid credentials",
                List.of()
        );
    }

    @ExceptionHandler(ApiUnauthorizedException.class)
    public ResponseEntity<ApiErrorResponse> handleApiUnauthorized(ApiUnauthorizedException ex) {
        return createErrorResponse(
                HttpStatus.UNAUTHORIZED,
                ApiErrorCodes.UNAUTHORIZED,
                ex.getMessage(),
                ex.getFieldErrors()
        );
    }

    // 403 - Forbidden (security / business rule)

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        return createErrorResponse(
                HttpStatus.FORBIDDEN,
                ApiErrorCodes.FORBIDDEN,
                "Forbidden",
                List.of()
        );
    }

    // 409 - Conflict

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiErrorResponse> handleConflict(IllegalStateException ex) {
        return createErrorResponse(
                HttpStatus.CONFLICT,
                ApiErrorCodes.CONFLICT,
                ex.getMessage(),
                List.of()
        );
    }

    @ExceptionHandler(ApiConflictException.class)
    public ResponseEntity<ApiErrorResponse> handleApiConflict(ApiConflictException ex) {
        return createErrorResponse(
                HttpStatus.CONFLICT,
                ApiErrorCodes.CONFLICT,
                ex.getMessage(),
                ex.getFieldErrors()
        );
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleDbConflict(DataIntegrityViolationException ex) {
        // No SQL/constraint leak
        return createErrorResponse(
                HttpStatus.CONFLICT,
                ApiErrorCodes.CONFLICT,
                "Conflict",
                List.of()
        );
    }

    // 500 - Internal

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGeneric(Exception ex) {
        log.error("Unhandled exception", ex);
        return createErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ApiErrorCodes.INTERNAL,
                "Internal error",
                List.of()
        );
    }

    // 404 - Not found
    @ExceptionHandler(ApiNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(ApiNotFoundException ex) {
        return createErrorResponse(
                HttpStatus.NOT_FOUND,
                ApiErrorCodes.NOT_FOUND,
                ex.getMessage(),
                List.of()
        );
    }

    private FieldErrorItem toFieldErrorItem(FieldError fe) {
        return new FieldErrorItem(fe.getField(), fe.getDefaultMessage());
    }
}
