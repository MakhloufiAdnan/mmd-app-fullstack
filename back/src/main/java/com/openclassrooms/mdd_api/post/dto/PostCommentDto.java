package com.openclassrooms.mdd_api.post.dto;

import java.time.Instant;

/**
 * Commentaire renvoyé dans le détail d'un post.
 */
public record PostCommentDto(
        Long id,
        String content,
        PostAuthorDto author,
        Instant createdAt
) {}
