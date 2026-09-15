package com.smartflow.mapper;

import com.smartflow.dto.InterventionDtos;
import com.smartflow.entity.Intervention;

/**
 * Mapper de l'entité Intervention vers ses DTOs.
 */
public final class InterventionMapper {

    private InterventionMapper() {
    }

    public static InterventionDtos.InterventionResponse toResponse(Intervention intervention) {
        String clientName = intervention.getClient().getUser().getFirstName()
                + " " + intervention.getClient().getUser().getLastName();
        String technicianName = intervention.getTechnician() != null
                ? intervention.getTechnician().getUser().getFirstName()
                + " " + intervention.getTechnician().getUser().getLastName()
                : null;

        return new InterventionDtos.InterventionResponse(
                intervention.getId(),
                intervention.getTitle(),
                intervention.getDescription(),
                intervention.getStatus(),
                intervention.getPriority(),
                intervention.getCategory().getId(),
                intervention.getCategory().getName(),
                intervention.getClient().getId(),
                clientName,
                intervention.getTechnician() != null ? intervention.getTechnician().getId() : null,
                technicianName,
                intervention.getLocation(),
                intervention.getCreatedAt(),
                intervention.getPlannedDate(),
                intervention.getEstimatedTimeMinutes(),
                intervention.getActualTimeMinutes(),
                intervention.getReport(),
                intervention.getClosedAt()
        );
    }
}