package com.smartflow.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * DTOs de gestion des catégories.
 */
public final class CategoryDtos {

    private CategoryDtos() {
    }

    public record CategoryResponse(
            Long id,
            String name,
            String description,
            String color,
            String icon
    ) {
    }

    public record CategoryRequest(
            @NotBlank(message = "Le nom est obligatoire") String name,
            String description,
            String color,
            String icon
    ) {
    }
}