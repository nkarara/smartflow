package com.smartflow.service;

import com.smartflow.dto.InterventionDtos;
import com.smartflow.dto.TechnicianDtos;
import com.smartflow.entity.Intervention;
import com.smartflow.entity.Priority;
import com.smartflow.entity.Role;
import com.smartflow.entity.Skill;
import com.smartflow.entity.Status;
import com.smartflow.entity.Technician;
import com.smartflow.entity.User;
import com.smartflow.exception.BusinessException;
import com.smartflow.exception.ResourceNotFoundException;
import com.smartflow.mapper.UserMapper;
import com.smartflow.repository.InterventionRepository;
import com.smartflow.repository.SkillRepository;
import com.smartflow.repository.TechnicianRepository;
import com.smartflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Gestion des techniciens.
 */
@Service
@RequiredArgsConstructor
public class TechnicianService {

    private final TechnicianRepository technicianRepository;
    private final UserRepository userRepository;
    private final SkillRepository skillRepository;
    private final InterventionRepository interventionRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public List<TechnicianDtos.TechnicianItem> listAll() {
        return technicianRepository.findAllByOrderByUser_LastNameAsc().stream()
                .map(UserMapper::toTechnicianItem)
                .toList();
    }

    @Transactional(readOnly = true)
    public TechnicianDtos.TechnicianItem get(Long id) {
        return UserMapper.toTechnicianItem(find(id));
    }

    @Transactional
    public TechnicianDtos.TechnicianItem create(TechnicianDtos.CreateTechnicianRequest request) {
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new BusinessException(HttpStatus.CONFLICT, "Un compte existe déjà avec cet email");
        }
        User user = User.builder()
                .email(request.email().trim().toLowerCase())
                .password(passwordEncoder.encode(request.password()))
                .firstName(request.firstName())
                .lastName(request.lastName())
                .phone(request.phone())
                .role(Role.TECHNICIAN)
                .enabled(true)
                .build();
        user = userRepository.save(user);
        Technician technician = Technician.builder()
                .user(user)
                .specialty(request.specialty())
                .location(request.location())
                .available(request.available() == null || request.available())
                .build();
        if (request.skills() != null) {
            request.skills().forEach(name -> addSkill(technician, name));
        }
        return UserMapper.toTechnicianItem(technicianRepository.save(technician));
    }

    @Transactional
    public TechnicianDtos.TechnicianItem update(Long id, TechnicianDtos.UpdateTechnicianRequest request) {
        Technician technician = find(id);
        if (request.phone() != null) {
            technician.getUser().setPhone(request.phone());
        }
        if (request.specialty() != null) {
            technician.setSpecialty(request.specialty());
        }
        if (request.location() != null) {
            technician.setLocation(request.location());
        }
        if (request.available() != null) {
            technician.setAvailable(request.available());
        }
        if (request.skills() != null) {
            technician.getSkills().clear();
            request.skills().forEach(name -> addSkill(technician, name));
        }
        return UserMapper.toTechnicianItem(technician);
    }

    @Transactional
    public void delete(Long id) {
        Technician technician = find(id);
        if (!interventionRepository.findByTechnicianIdAndStatusNot(technician.getId(),
                com.smartflow.entity.Status.CLOSED).isEmpty()) {
            throw new BusinessException(HttpStatus.CONFLICT,
                    "Impossible de supprimer le technicien : il a des interventions en cours");
        }
        userRepository.delete(technician.getUser());
        technicianRepository.delete(technician);
    }

    @Transactional(readOnly = true)
    public Technician findTechnicianByUserId(Long userId) {
        return technicianRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Technicien (userId)", userId));
    }

    public Technician find(Long id) {
        return technicianRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Technicien", id));
    }

    private void addSkill(Technician technician, String skillName) {
        String trimmed = skillName.trim();
        Skill skill = skillRepository.findByNameIgnoreCase(trimmed)
                .orElseGet(() -> skillRepository.save(Skill.builder().name(trimmed).build()));
        technician.getSkills().add(skill);
    }

    /**
     * Proposition automatique de technicien selon ses compétences, sa disponibilité,
     * sa localisation et sa charge de travail (nombre d'interventions en cours).
     */
    @Transactional(readOnly = true)
    public List<InterventionDtos.SuggestionItem> suggestFor(Intervention intervention) {
        String text = (intervention.getTitle() + " " + intervention.getDescription()).toLowerCase();
        List<Status> activeStatuses = List.of(Status.ASSIGNED, Status.ACCEPTED, Status.IN_PROGRESS, Status.BLOCKED);

        return technicianRepository.findAll().stream()
                .map(technician -> {
                    int score = 0;
                    List<String> reasons = new ArrayList<>();
                    long active = interventionRepository.countByTechnicianIdAndStatusIn(
                            technician.getId(), activeStatuses);

                    if (technician.isAvailable()) {
                        score += 5;
                        reasons.add("Disponible");
                    } else {
                        score -= 8;
                        reasons.add("Indisponible");
                    }

                    if (intervention.getLocation() != null
                            && technician.getLocation() != null
                            && intervention.getLocation().equalsIgnoreCase(technician.getLocation())) {
                        score += 3;
                        reasons.add("Localisation identique (" + technician.getLocation() + ")");
                    }

                    long matchedSkills = technician.getSkills().stream()
                            .filter(skill -> text.contains(skill.getName().toLowerCase()))
                            .count();
                    if (matchedSkills > 0) {
                        score += (int) matchedSkills * 6;
                        reasons.add("Compétences pertinentes (" + matchedSkills + ")");
                    }

                    score -= Math.min(active, 5) * 2;
                    if (active > 0) {
                        reasons.add(active + " intervention(s) en cours");
                    }

                    if (intervention.getPriority() == Priority.HIGH) {
                        score += 2;
                    } else if (intervention.getPriority() == Priority.URGENT) {
                        score += 3;
                    }

                    return new InterventionDtos.SuggestionItem(
                            technician.getId(),
                            technician.getUser().getFirstName() + " " + technician.getUser().getLastName(),
                            score,
                            technician.isAvailable(),
                            active,
                            reasons);
                })
                .sorted(Comparator.comparingInt(InterventionDtos.SuggestionItem::score).reversed())
                .toList();
    }
}