package com.smartflow.dto;

import com.smartflow.entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * DTOs liés à l'authentification.
 */
public final class AuthDtos {

    private AuthDtos() {
    }

    public record LoginRequest(
            @NotBlank(message = "L'email est obligatoire") @Email String email,
            @NotBlank(message = "Le mot de passe est obligatoire") String password
    ) {
    }

    /**
     * Inscription publique : seuls les rôles CLIENT et TECHNICIAN sont autorisés.
     */
    public record RegisterRequest(
            @NotBlank(message = "L'email est obligatoire") @Email String email,
            @NotBlank(message = "Le mot de passe est obligatoire")
            @Size(min = 8, message = "Le mot de passe doit contenir au moins 8 caractères") String password,
            @NotBlank(message = "Le prénom est obligatoire") String firstName,
            @NotBlank(message = "Le nom est obligatoire") String lastName,
            String phone,
            @NotNull(message = "Le rôle est obligatoire") Role role,
            String companyName,
            String location,
            List<String> skills
    ) {
    }

    public record RefreshRequest(@NotBlank String refreshToken) {
    }

    /**
     * Réponse d'authentification : access token + refresh token + profil utilisateur.
     */
    public record AuthResponse(
            String accessToken,
            String refreshToken,
            AuthUser user
    ) {
    }

    public record AuthUser(
            Long id,
            String email,
            String firstName,
            String lastName,
            Role role
    ) {
    }
}