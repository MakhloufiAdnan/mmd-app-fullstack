package com.openclassrooms.mdd_api.post.dto;

/**
 * Sous-objet "author" exposé dans le détail d'un post (et des commentaires).
 */
public record PostAuthorDto(
        Long id,
        String username
) {}
