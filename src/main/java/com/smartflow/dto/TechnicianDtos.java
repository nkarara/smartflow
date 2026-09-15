package com.smartflow.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * DTOs de gestion des techniciens.
 */
public final class TechnicianDtos {

    private TechnicianDtos() {
    }

    public record TechnicianItem(
            Long id,
            Long userId,
            String email,
            String firstName,
            String lastName,
            String phone,
            String specialty,
            String location,
            boolean available,
            List<String> skills
    ) {
    }

    public record CreateTechnicianRequest(
            @NotBlank(message = "L'email est obligatoire") @Email String email,
            @NotBlank(message = "Le mot de passe est obligatoire")
            @Size(min = 8, message = "Le mot de passe doit contenir au moins 8 caractères") String password,
            @NotBlank(message = "Le nom est obligatoire") String firstName,
            @NotBlank(message = "Le prénom est obligatoire") String lastName,
            String phone,
            String specialty,
            String location,
            Boolean available,
            List<String> skills
    ) {
    }

    public record UpdateTechnicianRequest(
            String phone,
            String specialty,
            String location,
            Boolean available,
            List<String> skills
    ) {
    }
}