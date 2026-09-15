package com.smartflow.dto;

import com.smartflow.entity.Priority;
import jakarta.validation.constraints.NotBlank;

/**
 * DTOs des fonctionnalités d'intelligence artificielle.
 */
public final class AiDtos {

    private AiDtos() {
    }

    /**
     * Analyse automatique d'une demande client : catégorie, priorité, temps estimé, problème probable.
     */
    public record AnalyzeRequest(
            String title,
            @NotBlank(message = "La description est obligatoire") String description
    ) {
    }

    public record AnalyzeResponse(
            String category,
            Long categoryId,
            String type,
            Priority priority,
            String probableProblem,
            Integer estimatedTimeMinutes,
            String analyzer
    ) {
    }

    /**
     * Génération d'un résumé à partir du compte rendu du technicien.
     */
    public record SummarizeRequest(
            @NotBlank(message = "Le compte rendu est obligatoire") String reportText,
            String title,
            String actions
    ) {
    }

    public record SummarizeResponse(String summary, String analyzer) {
    }
}