package com.openclassrooms.mdd_api.auth.dto;

public record TokenResponse(
        String accessToken,
        String tokenType,
        long expiresInSeconds
) {}
