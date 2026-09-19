package com.smartflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTOs de gestion des paramètres de l'application.
 */
public final class AppSettingDtos {

    private AppSettingDtos() {
    }

    /**
     * Représentation d'un paramètre exposée par l'API.
     *
     * @param key         nom unique du paramètre (ex. {@code support.email})
     * @param value       valeur courante
     * @param description utilité du paramètre
     * @param category    catégorie d'affichage
     */
    public record SettingResponse(
            String key,
            String value,
            String description,
            String category
    ) {
    }

    /**
     * Corps de requête de mise à jour : seule la valeur est modifiable.
     *
     * @param value nouvelle valeur du paramètre
     */
    public record UpdateSettingRequest(
            @NotBlank(message = "La valeur est obligatoire")
            @Size(max = 1000, message = "La valeur ne doit pas dépasser 1000 caractères") String value
    ) {
    }
}