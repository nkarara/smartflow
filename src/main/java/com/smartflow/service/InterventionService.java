package com.smartflow.service;

import com.smartflow.dto.InterventionDtos;
import com.smartflow.entity.Client;
import com.smartflow.entity.Intervention;
import com.smartflow.entity.InterventionHistory;
import com.smartflow.entity.NotificationType;
import com.smartflow.entity.Role;
import com.smartflow.entity.Status;
import com.smartflow.entity.Technician;
import com.smartflow.entity.User;
import com.smartflow.exception.BusinessException;
import com.smartflow.exception.ResourceNotFoundException;
import com.smartflow.mapper.InterventionMapper;
import com.smartflow.repository.InterventionHistoryRepository;
import com.smartflow.repository.InterventionRepository;
import com.smartflow.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * Gestion du cycle de vie des interventions : création, affectation, transitions de statut,
 * compte rendu, historique. Toutes les actions vérifient les droits selon le rôle.
 */
@Service
@RequiredArgsConstructor
public class InterventionService {

    private final InterventionRepository interventionRepository;
    private final InterventionHistoryRepository historyRepository;
    private final ClientService clientService;
    private final TechnicianService technicianService;
    private final CategoryService categoryService;
    private final NotificationService notificationService;

    private static final Set<Status> TECHNICIAN_STATUSES = Set.of(
            Status.ACCEPTED, Status.IN_PROGRESS, Status.BLOCKED, Status.RESOLVED);

    @Transactional(readOnly = true)
    public List<InterventionDtos.InterventionResponse> listAllForCurrentUser() {
        User user = SecurityUtils.currentUser();
        List<Intervention> interventions = switch (user.getRole()) {
            case ADMIN, MANAGER -> interventionRepository.findAll(
                    org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt"));
            case CLIENT -> interventionRepository.findByClientIdOrderByCreatedAtDesc(
                    clientService.findClientByUserId(user.getId()).getId());
            case TECHNICIAN -> interventionRepository.findByTechnicianIdOrderByCreatedAtDesc(
                    technicianService.findTechnicianByUserId(user.getId()).getId());
        };
        return interventions.stream().map(InterventionMapper::toResponse).toList();
    }

    @Transactional
    public InterventionDtos.InterventionResponse create(InterventionDtos.CreateRequest request) {
        User actor = SecurityUtils.currentUser();
        Client client;
        if (actor.getRole() == Role.CLIENT) {
            client = clientService.findClientByUserId(actor.getId());
        } else {
            if (request.clientId() == null) {
                throw new BusinessException("Le champ clientId est obligatoire pour cette opération");
            }
            client = clientService.find(request.clientId());
        }

        Intervention intervention = Intervention.builder()
                .title(request.title().trim())
                .description(request.description().trim())
                .client(client)
                .category(categoryService.find(request.categoryId()))
                .priority(request.priority())
                .location(request.location())
                .plannedDate(request.plannedDate())
                .estimatedTimeMinutes(request.estimatedTimeMinutes())
                .status(Status.NOUVELLE)
                .build();
        Intervention saved = interventionRepository.save(intervention);

        addHistory(saved, null, Status.NOUVELLE, "Intervention créée", actor);
        notificationService.notifyRole(Role.MANAGER, NotificationType.NEW_INTERVENTION,
                "Nouvelle intervention \"" + saved.getTitle() + "\" créée par le client "
                        + client.getUser().getFirstName() + " " + client.getUser().getLastName(),
                saved.getId());
        return InterventionMapper.toResponse(saved);
    }

    @Transactional
    public InterventionDtos.InterventionResponse update(Long id, InterventionDtos.UpdateRequest request) {
        Intervention intervention = findWithEditAccess(id);
        intervention.setTitle(request.title().trim());
        intervention.setDescription(request.description().trim());
        intervention.setCategory(categoryService.find(request.categoryId()));
        intervention.setPriority(request.priority());
        intervention.setLocation(request.location());
        intervention.setPlannedDate(request.plannedDate());
        intervention.setEstimatedTimeMinutes(request.estimatedTimeMinutes());
        return InterventionMapper.toResponse(intervention);
    }

    @Transactional
    public void delete(Long id) {
        requireRole(Role.ADMIN);
        interventionRepository.delete(find(id));
    }

    @Transactional
    public InterventionDtos.InterventionResponse assign(Long id, InterventionDtos.AssignRequest request) {
        requireRole(Role.ADMIN, Role.MANAGER);
        Intervention intervention = find(id);
        if (intervention.getStatus() != Status.NOUVELLE && intervention.getStatus() != Status.ASSIGNED) {
            throw new BusinessException(HttpStatus.CONFLICT,
                    "Seules les interventions NOUVELLE ou ASSIGNÉE peuvent être affectées (actuel: "
                            + intervention.getStatus() + ")");
        }
        Technician technician = technicianService.find(request.technicianId());
        Status from = intervention.getStatus();
        intervention.setTechnician(technician);
        intervention.setStatus(Status.ASSIGNED);
        addHistory(intervention, from, Status.ASSIGNED,
                "Affectation au technicien " + technician.getUser().getFirstName()
                        + " " + technician.getUser().getLastName(), SecurityUtils.currentUser());

        notificationService.notify(technician.getUser().getId(), NotificationType.INTERVENTION_ASSIGNED,
                "Une intervention vous a été affectée : \"" + intervention.getTitle() + "\"", intervention.getId());
        notificationService.notify(intervention.getClient().getUser().getId(), NotificationType.INTERVENTION_ASSIGNED,
                "Votre intervention \"" + intervention.getTitle() + "\" a été prise en charge", intervention.getId());
        return InterventionMapper.toResponse(intervention);
    }

    @Transactional
    public List<InterventionDtos.SuggestionItem> suggestTechnicians(Long id) {
        requireRole(Role.ADMIN, Role.MANAGER);
        return technicianService.suggestFor(find(id));
    }

    @Transactional
    public InterventionDtos.InterventionResponse changeStatus(Long id, InterventionDtos.StatusChangeRequest request) {
        User actor = SecurityUtils.currentUser();
        Intervention intervention = find(id);
        Status from = intervention.getStatus();
        Status to = request.newStatus();

        boolean isTechnician = actor.getRole() == Role.TECHNICIAN;
        if (isTechnician) {
            if (intervention.getTechnician() == null
                    || !intervention.getTechnician().getUser().getId().equals(actor.getId())) {
                throw new BusinessException(HttpStatus.FORBIDDEN,
                        "Vous ne pouvez modifier que vos propres interventions");
            }
            boolean refusal = from == Status.ASSIGNED && to == Status.NOUVELLE;
            if (!TECHNICIAN_STATUSES.contains(to) && !refusal) {
                throw new BusinessException(HttpStatus.FORBIDDEN,
                        "Un technicien ne peut pas passer au statut " + to);
            }
        } else {
            requireRole(Role.ADMIN, Role.MANAGER);
        }

        if (!Status.canTransition(from, to)) {
            throw new BusinessException(HttpStatus.CONFLICT,
                    "Transition de statut invalide : " + from.getLabel() + " -> " + to.getLabel());
        }

        intervention.setStatus(to);
        if (to == Status.CLOSED && intervention.getClosedAt() == null) {
            intervention.setClosedAt(LocalDateTime.now());
        }
        if (to != Status.CLOSED) {
            intervention.setClosedAt(null);
        }

        addHistory(intervention, from, to, request.comment(), actor);
        if (isTechnician && to == Status.NOUVELLE) {
            intervention.setTechnician(null);
            notificationService.notifyRole(Role.MANAGER, NotificationType.STATUS_CHANGED,
                    "Le technicien a refusé l'intervention \"" + intervention.getTitle() + "\"",
                    intervention.getId());
        }
        sendStatusNotifications(intervention, from, to);
        return InterventionMapper.toResponse(intervention);
    }

    @Transactional
    public InterventionDtos.InterventionResponse submitAccount(Long id, InterventionDtos.AccountRequest request) {
        User actor = SecurityUtils.currentUser();
        Intervention intervention = find(id);
        if (actor.getRole() == Role.TECHNICIAN) {
            if (intervention.getTechnician() == null
                    || !intervention.getTechnician().getUser().getId().equals(actor.getId())) {
                throw new BusinessException(HttpStatus.FORBIDDEN,
                        "Vous ne pouvez ajouter un compte rendu que sur vos propres interventions");
            }
        } else {
            requireRole(Role.ADMIN, Role.MANAGER);
        }
        if (request.actualTimeMinutes() != null && request.actualTimeMinutes() >= 0) {
            intervention.setActualTimeMinutes(request.actualTimeMinutes());
        }
        if (request.report() != null && !request.report().isBlank()) {
            intervention.setReport(request.report().trim());
        }
        notificationService.notify(intervention.getClient().getUser().getId(), NotificationType.STATUS_CHANGED,
                "Un compte rendu a été ajouté à l'intervention \"" + intervention.getTitle() + "\"",
                intervention.getId());
        return InterventionMapper.toResponse(intervention);
    }

    @Transactional(readOnly = true)
    public InterventionDtos.InterventionResponse get(Long id) {
        return InterventionMapper.toResponse(getAccessible(id));
    }

    public void requireViewAccess(Long interventionId) {
        getAccessible(interventionId);
    }

    @Transactional(readOnly = true)
    public List<InterventionDtos.HistoryResponse> getHistory(Long id) {
        requireViewAccess(id);
        return historyRepository.findByInterventionIdOrderByChangedAtAsc(id).stream()
                .map(h -> new InterventionDtos.HistoryResponse(
                        h.getId(),
                        h.getChangedBy() != null ? h.getChangedBy().getId() : null,
                        h.getChangedBy() != null
                                ? h.getChangedBy().getFirstName() + " " + h.getChangedBy().getLastName()
                                : "Système",
                        h.getFromStatus(),
                        h.getToStatus(),
                        h.getComment(),
                        h.getChangedAt()))
                .toList();
    }

    private void sendStatusNotifications(Intervention intervention, Status from, Status to) {
        String title = intervention.getTitle();
        Long clientUserId = intervention.getClient().getUser().getId();
        switch (to) {
            case ACCEPTED -> {
                notificationService.notify(clientUserId, NotificationType.INTERVENTION_ACCEPTED,
                        "Le technicien a accepté votre intervention \"" + title + "\"", intervention.getId());
                notificationService.notifyRole(Role.MANAGER, NotificationType.INTERVENTION_ACCEPTED,
                        "Intervention \"" + title + "\" acceptée", intervention.getId());
            }
            case IN_PROGRESS -> notificationService.notify(clientUserId, NotificationType.STATUS_CHANGED,
                    "L'intervention \"" + title + "\" est en cours", intervention.getId());
            case BLOCKED -> {
                notificationService.notify(clientUserId, NotificationType.STATUS_CHANGED,
                        "L'intervention \"" + title + "\" est bloquée", intervention.getId());
                notificationService.notifyRole(Role.MANAGER, NotificationType.STATUS_CHANGED,
                        "Intervention bloquée : \"" + title + "\"", intervention.getId());
            }
            case RESOLVED -> {
                notificationService.notify(clientUserId, NotificationType.INTERVENTION_RESOLVED,
                        "Votre intervention \"" + title + "\" a été résolue", intervention.getId());
                notificationService.notifyRole(Role.MANAGER, NotificationType.INTERVENTION_RESOLVED,
                        "Intervention résolue : \"" + title + "\"", intervention.getId());
            }
            case CLOSED -> notificationService.notify(clientUserId, NotificationType.INTERVENTION_CLOSED,
                    "Votre intervention \"" + title + "\" est clôturée. Vous pouvez laisser une évaluation",
                    intervention.getId());
            default -> {
            }
        }
    }

    private void requireRole(Role... roles) {
        Role current = SecurityUtils.currentUser().getRole();
        for (Role role : roles) {
            if (current == role) {
                return;
            }
        }
        throw new BusinessException(HttpStatus.FORBIDDEN, "Rôle insuffisant");
    }

    private void addHistory(Intervention intervention, Status from, Status to, String comment, User actor) {
        historyRepository.save(InterventionHistory.builder()
                .intervention(intervention)
                .changedBy(actor)
                .fromStatus(from)
                .toStatus(to)
                .comment(comment)
                .build());
    }

    public Intervention find(Long id) {
        return interventionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Intervention", id));
    }

    private boolean canView(Intervention intervention, User user) {
        return switch (user.getRole()) {
            case ADMIN, MANAGER -> true;
            case CLIENT -> intervention.getClient().getUser().getId().equals(user.getId());
            case TECHNICIAN -> intervention.getTechnician() != null
                    && intervention.getTechnician().getUser().getId().equals(user.getId());
        };
    }

    public Intervention getAccessible(Long id) {
        Intervention intervention = find(id);
        User user = SecurityUtils.currentUser();
        if (!canView(intervention, user)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "Vous n'avez pas accès à cette intervention");
        }
        return intervention;
    }

    private Intervention findWithEditAccess(Long id) {
        requireRole(Role.ADMIN, Role.MANAGER);
        return find(id);
    }
}