package com.smartflow.dto;

import com.smartflow.entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTOs de gestion des utilisateurs (admin).
 */
public final class UserDtos {

    private UserDtos() {
    }

    public record UserResponse(
            Long id,
            String email,
            String firstName,
            String lastName,
            String phone,
            Role role,
            boolean enabled,
            LocalDateTime createdAt,
            Long clientId,
            Long technicianId
    ) {
    }

    public record CreateUserRequest(
            @NotBlank(message = "L'email est obligatoire") @Email String email,
            @NotBlank(message = "Le mot de passe est obligatoire")
            @Size(min = 8, message = "Le mot de passe doit contenir au moins 8 caractères") String password,
            @NotBlank(message = "Le prénom est obligatoire") String firstName,
            @NotBlank(message = "Le nom est obligatoire") String lastName,
            String phone,
            @NotNull(message = "Le rôle est obligatoire") Role role,
            String companyName,
            String location,
            String specialty,
            List<String> skills
    ) {
    }

    public record UpdateUserRequest(
            String firstName,
            String lastName,
            String phone,
            Boolean enabled
    ) {
    }
}