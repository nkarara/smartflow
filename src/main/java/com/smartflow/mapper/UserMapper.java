package com.smartflow.mapper;

import com.smartflow.dto.ClientDtos;
import com.smartflow.dto.TechnicianDtos;
import com.smartflow.dto.UserDtos;
import com.smartflow.entity.Client;
import com.smartflow.entity.Technician;
import com.smartflow.entity.User;

import java.util.Comparator;
import java.util.List;

/**
 * Mapper des entités utilisateur/client/technicien vers leurs DTOs.
 */
public final class UserMapper {

    private UserMapper() {
    }

    public static UserDtos.UserResponse toUserResponse(User user, Long clientId, Long technicianId) {
        return new UserDtos.UserResponse(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getPhone(),
                user.getRole(),
                user.isEnabled(),
                user.getCreatedAt(),
                clientId,
                technicianId
        );
    }

    public static ClientDtos.ClientResponse toClientResponse(Client client) {
        User user = client.getUser();
        return new ClientDtos.ClientResponse(
                client.getId(),
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getPhone(),
                client.getCompanyName(),
                client.getAddress(),
                client.getCity(),
                client.getSiret()
        );
    }

    public static TechnicianDtos.TechnicianItem toTechnicianItem(Technician technician) {
        User user = technician.getUser();
        List<String> skills = technician.getSkills().stream()
                .map(s -> s.getName())
                .sorted(Comparator.naturalOrder())
                .toList();
        return new TechnicianDtos.TechnicianItem(
                technician.getId(),
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getPhone(),
                technician.getSpecialty(),
                technician.getLocation(),
                technician.isAvailable(),
                skills
        );
    }
}