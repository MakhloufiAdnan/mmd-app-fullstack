package com.openclassrooms.mdd_api.post.dto;

/**
 * Sous-objet "topic" exposé dans le détail d'un post.
 */
public record PostTopicDto(
        Long id,
        String name
) {}
