package com.openclassrooms.mdd_api.common.web;

import java.util.List;

/**
 * Exception métier -> HTTP 401.
 *
 * Usage "prod" :
 * - refresh token absent/invalide/expiré
 * - cas où on veut un message 401 propre, sans fuite d'informations.
 */
public class ApiUnauthorizedException extends RuntimeException {

    private final List<FieldErrorItem> fieldErrors;

    public ApiUnauthorizedException(String message) {
        this(message, List.of());
    }

    public ApiUnauthorizedException(String message, List<FieldErrorItem> fieldErrors) {
        super(message);
        this.fieldErrors = fieldErrors == null ? List.of() : List.copyOf(fieldErrors);
    }

    public List<FieldErrorItem> getFieldErrors() {
        return fieldErrors;
    }
}
