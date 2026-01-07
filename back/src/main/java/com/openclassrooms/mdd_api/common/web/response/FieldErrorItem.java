package com.openclassrooms.mdd_api.common.web.response;

public record FieldErrorItem(
        String field,
        String message
) {}
