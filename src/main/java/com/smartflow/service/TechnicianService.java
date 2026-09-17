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
 * Gestion des techniciens : CRUD + moteur de <b>suggestion automatique</b>
 * pour l'affectation des interventions.
 *
 * <p>La méthode {@link #suggestFor} attribue un score à chaque technicien selon :
 * <ol>
 *   <li>sa disponibilité ;</li>
 *   <li>la correspondance de localisation avec le site de l'intervention ;</li>
 *   <li>ses compétences présentes dans le titre/description de l'intervention ;</li>
 *   <li>sa charge de travail (interventions actives) ;</li>
 *   <li>la priorité de l'intervention (bonus pour HIGH/URGENT).</li>
 * </ol>
 * </p>
 */
@Service
@RequiredArgsConstructor
public class TechnicianService {

    /** Dépôt d'accès aux techniciens. */
    private final TechnicianRepository technicianRepository;
    /** Dépôt d'accès aux comptes utilisateurs. */
    private final UserRepository userRepository;
    /** Dépôt d'accès aux compétences. */
    private final SkillRepository skillRepository;
    /** Dépôt d'accès aux interventions (calcul de charge). */
    private final InterventionRepository interventionRepository;
    /** Encodeur BCrypt pour les mots de passe des techniciens créés. */
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
     * Proposition automatique de technicien pour une intervention donnée.
     * Chaque technicien reçoit un score : disponibilité, localisation, compétences,
     * charge de travail et priorité. La liste est triée du meilleur au moins bon.
     *
     * @param intervention l'intervention à traiter
     * @return les techniciens triés par score décroissant avec les raisons
     */
    @Transactional(readOnly = true)
    public List<InterventionDtos.SuggestionItem> suggestFor(Intervention intervention) {
        // Texte combiné (titre + description) en minuscules pour comparer les compétences
        String text = (intervention.getTitle() + " " + intervention.getDescription()).toLowerCase();
        // Statuts considérés comme « actifs » (qui pèsent sur la charge du technicien)
        List<Status> activeStatuses = List.of(Status.ASSIGNED, Status.ACCEPTED, Status.IN_PROGRESS, Status.BLOCKED);

        return technicianRepository.findAll().stream()
                .map(technician -> {
                    int score = 0;
                    List<String> reasons = new ArrayList<>();
                    // Charge actuelle : nombre d'interventions en cours du technicien
                    long active = interventionRepository.countByTechnicianIdAndStatusIn(
                            technician.getId(), activeStatuses);

                    // 1. Disponibilité : gros bonus/malus
                    if (technician.isAvailable()) {
                        score += 5;
                        reasons.add("Disponible");
                    } else {
                        score -= 8;
                        reasons.add("Indisponible");
                    }

                    // 2. Localisation identique → intervention plus rapide
                    if (intervention.getLocation() != null
                            && technician.getLocation() != null
                            && intervention.getLocation().equalsIgnoreCase(technician.getLocation())) {
                        score += 3;
                        reasons.add("Localisation identique (" + technician.getLocation() + ")");
                    }

                    // 3. Compétences : chaque compétence mentionnée dans le texte rapporte 6 points
                    long matchedSkills = technician.getSkills().stream()
                            .filter(skill -> text.contains(skill.getName().toLowerCase()))
                            .count();
                    if (matchedSkills > 0) {
                        score += (int) matchedSkills * 6;
                        reasons.add("Compétences pertinentes (" + matchedSkills + ")");
                    }

                    // 4. Charge de travail : chaque intervention active retire 2 points (max -10)
                    score -= Math.min(active, 5) * 2;
                    if (active > 0) {
                        reasons.add(active + " intervention(s) en cours");
                    }

                    // 5. Priorité : les urgences méritent un technicien rapidement mobilisable
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
                // Tri du meilleur score au plus faible
                .sorted(Comparator.comparingInt(InterventionDtos.SuggestionItem::score).reversed())
                .toList();
    }
}