package com.openclassrooms.mdd_api.post.dto;

import java.time.Instant;
import java.util.List;

/**
 * Réponse du détail d'un post.
 */
public record PostDetailResponse(
        Long id,
        PostTopicDto topic,
        String title,
        String content,
        PostAuthorDto author,
        Instant createdAt,
        List<PostCommentDto> comments
) {}
