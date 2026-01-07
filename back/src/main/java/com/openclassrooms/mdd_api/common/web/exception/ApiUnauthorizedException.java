package com.openclassrooms.mdd_api.common.web.exception;

import com.openclassrooms.mdd_api.common.web.response.FieldErrorItem;
import lombok.Getter;

import java.util.List;

/**
 * Exception métier pour les erreurs d'authentification (HTTP 401).
 */
@Getter
public class ApiUnauthorizedException extends RuntimeException {

    private final transient List<FieldErrorItem> fieldErrors;

    public ApiUnauthorizedException(String message) {
        this(message, List.of());
    }

    public ApiUnauthorizedException(String message, List<FieldErrorItem> fieldErrors) {
        super(message);
        this.fieldErrors = fieldErrors == null ? List.of() : List.copyOf(fieldErrors);
    }
}
