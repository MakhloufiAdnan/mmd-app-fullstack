package com.openclassrooms.mdd_api.common.web;

import java.util.List;

/**
 * Exception métier -> HTTP 400.
 *
 * Usage "prod" :
 * - pour toutes les validations non couvertes par Bean Validation (@Valid),
 *   ex: politique de mot de passe, règles métier.
 * - permet de renvoyer des fieldErrors cohérents avec le contrat API.
 */
public class ApiBadRequestException extends RuntimeException {

    private final List<FieldErrorItem> fieldErrors;

    public ApiBadRequestException(String message) {
        this(message, List.of());
    }

    public ApiBadRequestException(String message, List<FieldErrorItem> fieldErrors) {
        super(message);
        this.fieldErrors = fieldErrors == null ? List.of() : List.copyOf(fieldErrors);
    }

    public List<FieldErrorItem> getFieldErrors() {
        return fieldErrors;
    }
}
