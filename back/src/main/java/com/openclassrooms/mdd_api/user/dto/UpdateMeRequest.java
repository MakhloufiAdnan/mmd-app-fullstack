package com.openclassrooms.mdd_api.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/**
 * Mise à jour du profil.
 */
public record UpdateMeRequest(
        @Email @Size(max = 254) String email,
        @Size(max = 50) String username,
        @Size(min = 8, max = 72) String password
) {}
