package com.openclassrooms.mdd_api.common.web.exception;

import com.openclassrooms.mdd_api.common.web.response.FieldErrorItem;
import lombok.Getter;

import java.util.List;

/**
 * Exception métier pour gérer les erreurs de validation (HTTP 400).
 */
@Getter
public class ApiBadRequestException extends RuntimeException {

    private final transient List<FieldErrorItem> fieldErrors;

    public ApiBadRequestException(String message) {
        this(message, List.of());
    }

    public ApiBadRequestException(String message, List<FieldErrorItem> fieldErrors) {
        super(message);
        this.fieldErrors = fieldErrors == null ? List.of() : List.copyOf(fieldErrors);
    }
}
