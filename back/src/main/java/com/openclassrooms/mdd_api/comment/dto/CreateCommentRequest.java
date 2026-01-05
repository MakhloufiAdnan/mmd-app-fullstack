package com.openclassrooms.mdd_api.comment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Requête d'ajout de commentaire.
 * Contrat API (POST /api/posts/{postId}/comments) :
 */
public record CreateCommentRequest(
        @NotBlank @Size(max = 2_000) String content
) {}
