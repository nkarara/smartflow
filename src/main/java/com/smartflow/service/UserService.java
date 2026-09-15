package com.smartflow.service;

import com.smartflow.dto.UserDtos;
import com.smartflow.entity.Client;
import com.smartflow.entity.Role;
import com.smartflow.entity.Skill;
import com.smartflow.entity.Technician;
import com.smartflow.entity.User;
import com.smartflow.exception.BusinessException;
import com.smartflow.exception.ResourceNotFoundException;
import com.smartflow.mapper.UserMapper;
import com.smartflow.repository.ClientRepository;
import com.smartflow.repository.InterventionRepository;
import com.smartflow.repository.RefreshTokenRepository;
import com.smartflow.repository.SkillRepository;
import com.smartflow.repository.TechnicianRepository;
import com.smartflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Gestion des utilisateurs (droits administrateur).
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final ClientRepository clientRepository;
    private final TechnicianRepository technicianRepository;
    private final SkillRepository skillRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final InterventionRepository interventionRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public List<UserDtos.UserResponse> listAll() {
        return userRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt")).stream()
                .map(this::toResponseWithProfiles)
                .toList();
    }

    @Transactional
    public UserDtos.UserResponse create(UserDtos.CreateUserRequest request) {
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new BusinessException(HttpStatus.CONFLICT, "Un compte existe déjà avec cet email");
        }
        User user = User.builder()
                .email(request.email().trim().toLowerCase())
                .password(passwordEncoder.encode(request.password()))
                .firstName(request.firstName())
                .lastName(request.lastName())
                .phone(request.phone())
                .role(request.role())
                .enabled(true)
                .build();
        user = userRepository.save(user);
        attachProfile(user, request.role(), request.companyName(), request.location(), request.specialty(), request.skills());
        return toResponseWithProfiles(user);
    }

    @Transactional
    public UserDtos.UserResponse update(Long id, UserDtos.UpdateUserRequest request) {
        User user = find(id);
        if (request.firstName() != null && !request.firstName().isBlank()) {
            user.setFirstName(request.firstName().trim());
        }
        if (request.lastName() != null && !request.lastName().isBlank()) {
            user.setLastName(request.lastName().trim());
        }
        if (request.phone() != null) {
            user.setPhone(request.phone());
        }
        if (request.enabled() != null) {
            user.setEnabled(request.enabled());
        }
        return toResponseWithProfiles(userRepository.save(user));
    }

    @Transactional
    public void delete(Long id) {
        User user = find(id);
        if (user.getRole() == Role.ADMIN && userRepository.countByRole(Role.ADMIN) <= 1) {
            throw new BusinessException(HttpStatus.CONFLICT, "Impossible de supprimer le dernier administrateur");
        }
        clientRepository.findByUserId(id).ifPresent(client -> {
            if (interventionRepository.countByClientId(client.getId()) > 0) {
                throw new BusinessException(HttpStatus.CONFLICT,
                        "Impossible de supprimer le client : il possède des interventions");
            }
            clientRepository.delete(client);
        });
        technicianRepository.findByUserId(id).ifPresent(technicianRepository::delete);
        refreshTokenRepository.deleteByUserId(id);
        userRepository.delete(user);
    }

    public User find(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", id));
    }

    private UserDtos.UserResponse toResponseWithProfiles(User user) {
        Long clientId = clientRepository.findByUserId(user.getId()).map(Client::getId).orElse(null);
        Long technicianId = technicianRepository.findByUserId(user.getId()).map(Technician::getId).orElse(null);
        return UserMapper.toUserResponse(user, clientId, technicianId);
    }

    private void attachProfile(User user, Role role, String companyName, String location, String specialty, List<String> skills) {
        switch (role) {
            case CLIENT -> clientRepository.save(Client.builder()
                    .user(user)
                    .companyName(companyName)
                    .city(location)
                    .build());
            case TECHNICIAN -> {
                Technician technician = Technician.builder()
                        .user(user)
                        .specialty(specialty)
                        .location(location)
                        .available(true)
                        .build();
                if (skills != null) {
                    skills.forEach(name -> addSkill(technician, name));
                }
                technicianRepository.save(technician);
            }
            default -> {
                // ADMIN / MANAGER : aucun profil métier supplémentaire
            }
        }
    }

    private void addSkill(Technician technician, String skillName) {
        String trimmed = skillName.trim();
        Skill skill = skillRepository.findByNameIgnoreCase(trimmed)
                .orElseGet(() -> skillRepository.save(Skill.builder().name(trimmed).build()));
        technician.getSkills().add(skill);
    }
}