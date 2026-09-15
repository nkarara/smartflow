package com.smartflow.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/**
 * DTOs de gestion des évaluations.
 */
public final class RatingDtos {

    private RatingDtos() {
    }

    public record RatingRequest(
            @NotNull(message = "La note est obligatoire")
            @Min(value = 1, message = "La note doit être entre 1 et 5")
            @Max(value = 5, message = "La note doit être entre 1 et 5") Integer score,
            String comment
    ) {
    }

    public record RatingResponse(
            Long id,
            Long interventionId,
            int score,
            String comment,
            LocalDateTime createdAt
    ) {
    }
}