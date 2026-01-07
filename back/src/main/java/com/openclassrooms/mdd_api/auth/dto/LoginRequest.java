package com.openclassrooms.mdd_api.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO login.
 * "identifier" = email OU username (contrat).
 */
public record LoginRequest(
        @NotBlank @Size(max = 254) String identifier,
        @NotBlank @Size(min = 1, max = 72) String password
) {}
