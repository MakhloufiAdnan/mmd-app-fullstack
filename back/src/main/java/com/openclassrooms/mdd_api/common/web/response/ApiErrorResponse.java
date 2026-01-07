package com.openclassrooms.mdd_api.common.web.response;

import java.util.List;

public record ApiErrorResponse(
        String error,
        String message,
        List<FieldErrorItem> fieldErrors
) {}
