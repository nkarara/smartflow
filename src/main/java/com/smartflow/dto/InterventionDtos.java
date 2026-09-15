package com.smartflow.dto;

import com.smartflow.entity.Priority;
import com.smartflow.entity.Status;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTOs de gestion des interventions.
 */
public final class InterventionDtos {

    private InterventionDtos() {
    }

    public record CreateRequest(
            Long clientId,
            @NotBlank(message = "Le titre est obligatoire")
            @Size(max = 190, message = "Le titre ne doit pas dépasser 190 caractères") String title,
            @NotBlank(message = "La description est obligatoire") String description,
            @NotNull(message = "La catégorie est obligatoire") Long categoryId,
            @NotNull(message = "La priorité est obligatoire") Priority priority,
            String location,
            LocalDateTime plannedDate,
            Integer estimatedTimeMinutes
    ) {
    }

    public record UpdateRequest(
            @NotBlank(message = "Le titre est obligatoire") String title,
            @NotBlank(message = "La description est obligatoire") String description,
            @NotNull(message = "La catégorie est obligatoire") Long categoryId,
            @NotNull(message = "La priorité est obligatoire") Priority priority,
            String location,
            LocalDateTime plannedDate,
            Integer estimatedTimeMinutes
    ) {
    }

    public record AssignRequest(@NotNull(message = "Le technicien est obligatoire") Long technicianId) {
    }

    public record StatusChangeRequest(
            @NotNull(message = "Le statut est obligatoire") Status newStatus,
            String comment
    ) {
    }

    public record AccountRequest(
            @NotNull(message = "Le temps réel est obligatoire") Integer actualTimeMinutes,
            String report
    ) {
    }

    public record InterventionResponse(
            Long id,
            String title,
            String description,
            Status status,
            Priority priority,
            Long categoryId,
            String categoryName,
            Long clientId,
            String clientName,
            Long technicianId,
            String technicianName,
            String location,
            LocalDateTime createdAt,
            LocalDateTime plannedDate,
            Integer estimatedTimeMinutes,
            Integer actualTimeMinutes,
            String report,
            LocalDateTime closedAt
    ) {
    }

    public record SuggestionItem(
            Long technicianId,
            String technicianName,
            int score,
            boolean available,
            long activeInterventions,
            List<String> reasons
    ) {
    }

    public record HistoryResponse(
            Long id,
            Long changedById,
            String changedByName,
            Status fromStatus,
            Status toStatus,
            String comment,
            LocalDateTime changedAt
    ) {
    }
}