package com.openclassrooms.mdd_api.user.dto;

/**
 * Réponse d'update profil.
 * PUT /api/users/me -> { "updated": true }
 */
public record UpdatedResponse(
        boolean updated
) {}
