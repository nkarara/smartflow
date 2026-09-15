package com.smartflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * DTOs de gestion des clients.
 */
public final class ClientDtos {

    private ClientDtos() {
    }

    public record ClientResponse(
            Long id,
            Long userId,
            String email,
            String firstName,
            String lastName,
            String phone,
            String companyName,
            String address,
            String city,
            String siret
    ) {
    }

    public record CreateClientRequest(
            @NotBlank(message = "L'email est obligatoire") String email,
            @NotBlank(message = "Le mot de passe est obligatoire") String password,
            @NotBlank(message = "Le nom est obligatoire") String firstName,
            @NotBlank(message = "Le prénom est obligatoire") String lastName,
            String phone,
            @NotNull(message = "L'entreprise est obligatoire") String companyName,
            String address,
            String city,
            String siret
    ) {
    }

    public record UpdateClientRequest(
            String phone,
            String companyName,
            String address,
            String city,
            String siret
    ) {
    }
}