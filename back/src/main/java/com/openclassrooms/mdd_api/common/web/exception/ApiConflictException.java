package com.openclassrooms.mdd_api.common.web.exception;

import com.openclassrooms.mdd_api.common.web.response.FieldErrorItem;
import lombok.Getter;

import java.util.List;

/**
 * Exception métier pour les conflits d’unicité (HTTP 409).
 */
@Getter
public class ApiConflictException extends RuntimeException {

    private final transient List<FieldErrorItem> fieldErrors;

    public ApiConflictException(String message) {
        this(message, List.of());
    }

    public ApiConflictException(String message, List<FieldErrorItem> fieldErrors) {
        super(message);
        this.fieldErrors = fieldErrors == null ? List.of() : List.copyOf(fieldErrors);
    }
}
