package com.smartflow.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;

/**
 * DTOs de gestion des commentaires.
 */
public final class CommentDtos {

    private CommentDtos() {
    }

    public record CommentRequest(@NotBlank(message = "Le commentaire est obligatoire") String content) {
    }

    public record CommentResponse(
            Long id,
            Long authorId,
            String authorName,
            String content,
            LocalDateTime createdAt
    ) {
    }
}