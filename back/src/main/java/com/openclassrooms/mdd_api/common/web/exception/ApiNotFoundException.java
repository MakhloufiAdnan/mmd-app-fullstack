package com.openclassrooms.mdd_api.common.web.exception;

public class ApiNotFoundException extends RuntimeException {
    public ApiNotFoundException(String message) {

        super(message);
    }
}
