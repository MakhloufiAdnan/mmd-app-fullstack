package com.openclassrooms.mdd_api.common.web;

import java.util.List;

/**
 * Exception métier -> HTTP 409.
 *
 * Usage "prod" :
 * - conflits d’unicité (email/username déjà utilisé, déjà abonné, etc.)
 * - ne pas utiliser IllegalStateException (trop générique => risque de mauvais status).
 */
public class ApiConflictException extends RuntimeException {

    private final List<FieldErrorItem> fieldErrors;

    public ApiConflictException(String message) {
        this(message, List.of());
    }

    public ApiConflictException(String message, List<FieldErrorItem> fieldErrors) {
        super(message);
        this.fieldErrors = fieldErrors == null ? List.of() : List.copyOf(fieldErrors);
    }

    public List<FieldErrorItem> getFieldErrors() {
        return fieldErrors;
    }
}
