package com.openclassrooms.mdd_api.post.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Requête de création d'un post.
 * Contrat API (POST /api/posts) :
 */
public record CreatePostRequest(
        @NotNull @Positive Long topicId,
        @NotBlank @Size(max = 255) String title,
        @NotBlank @Size(max = 10_000) String content
) {}
