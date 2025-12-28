package com.openclassrooms.mdd_api.user.dto;

/**
 * Représentation minimale d'un topic (id + name).
 * Utilisé ici pour "subscriptions" dans le profil.
 */
public record TopicDto(
        Long id,
        String name
) {}
