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
 * compte rendu, historique. Toutes les actions vérifient les droits selon le rôle de
 * l'utilisateur connecté ({@code SecurityUtils#currentUser()}).
 *
 * <p>Points clés :
 * <ul>
 *   <li>chaque changement d'état est validé par la machine à états {@link Status#canTransition}
 *       puis journalisé dans l'historique ({@link InterventionHistory}) ;</li>
 *   <li>chaque événement important déclenche des notifications vers les acteurs concernés ;</li>
 *   <li>la visibilité (lecture) est restreinte : client → ses demandes, technicien → ses
 *       interventions, manager/admin → tout.</li>
 * </ul>
 * </p>
 */
@Service
@RequiredArgsConstructor
public class InterventionService {

    /** Dépôt d'accès aux interventions. */
    private final InterventionRepository interventionRepository;
    /** Dépôt d'accès à l'historique des changements de statut. */
    private final InterventionHistoryRepository historyRepository;
    /** Service de gestion des clients (profil courant, validation). */
    private final ClientService clientService;
    /** Service de gestion des techniciens (validation, suggestions). */
    private final TechnicianService technicianService;
    /** Service de gestion des catégories (validation). */
    private final CategoryService categoryService;
    /** Service de génération de notifications. */
    private final NotificationService notificationService;

    /** Statuts modifiables par un technicien (le reste nécessite manager/admin). */
    private static final Set<Status> TECHNICIAN_STATUSES = Set.of(
            Status.ACCEPTED, Status.IN_PROGRESS, Status.BLOCKED, Status.RESOLVED);

    /**
     * Liste les interventions visibles par l'utilisateur courant.
     * Client → ses demandes ; technicien → ses interventions ; manager/admin → toutes.
     *
     * @return les DTO d'interventions triés par date de création décroissante
     */
    @Transactional(readOnly = true)
    public List<InterventionDtos.InterventionResponse> listAllForCurrentUser() {
        User user = SecurityUtils.currentUser();
        // La portée dépend du rôle : switch d'expression (Java 21)
        List<Intervention> interventions = switch (user.getRole()) {
            case ADMIN, MANAGER -> interventionRepository.findAll(
                    org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt"));
            case CLIENT -> interventionRepository.findByClientIdOrderByCreatedAtDesc(
                    clientService.findClientByUserId(user.getId()).getId());
            case TECHNICIAN -> interventionRepository.findByTechnicianIdOrderByCreatedAtDesc(
                    technicianService.findTechnicianByUserId(user.getId()).getId());
        };
        // Conversion entité → DTO pour la sérialisation JSON
        return interventions.stream().map(InterventionMapper::toResponse).toList();
    }

    /**
     * Crée une nouvelle intervention au statut {@link Status#NOUVELLE}.
     * Le client est déduit du compte connecté, sinon exigé dans la requête (manager/admin).
     *
     * @param request données de création fournies par le client/formulaire
     * @return l'intervention créée (statut NOUVELLE)
     */
    @Transactional
    public InterventionDtos.InterventionResponse create(InterventionDtos.CreateRequest request) {
        User actor = SecurityUtils.currentUser();
        Client client;
        if (actor.getRole() == Role.CLIENT) {
            client = clientService.findClientByUserId(actor.getId()); // sa propre demande
        } else {
            if (request.clientId() == null) {
                throw new BusinessException("Le champ clientId est obligatoire pour cette opération");
            }
            client = clientService.find(request.clientId()); // manager/admin choisit un client
        }

        // Construction de l'intervention ; le @PrePersist applique NOUVELLE par défaut
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

        // Journalisation de la création dans l'historique (audit)
        addHistory(saved, null, Status.NOUVELLE, "Intervention créée", actor);
        // Alerte des managers pour qu'ils affectent un technicien au plus vite
        notificationService.notifyRole(Role.MANAGER, NotificationType.NEW_INTERVENTION,
                "Nouvelle intervention \"" + saved.getTitle() + "\" créée par le client "
                        + client.getUser().getFirstName() + " " + client.getUser().getLastName(),
                saved.getId());
        return InterventionMapper.toResponse(saved);
    }

    /**
     * Modifie les champs libres d'une intervention (titre, description, catégorie,
     * priorité, localisation, dates) — droits MANAGER/ADMIN exigés.
     *
     * @param id      identifiant de l'intervention
     * @param request nouvelles valeurs
     * @return l'intervention mise à jour
     */
    @Transactional
    public InterventionDtos.InterventionResponse update(Long id, InterventionDtos.UpdateRequest request) {
        // Vérifie que l'utilisateur a les droits de modification (ADMIN/MANAGER)
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

    /**
     * Supprime définitivement une intervention — droits ADMIN exigés.
     * Les collections en cascade (commentaires, pièces jointes, historique) sont supprimées avec elle.
     *
     * @param id identifiant de l'intervention
     */
    @Transactional
    public void delete(Long id) {
        requireRole(Role.ADMIN);
        interventionRepository.delete(find(id));
    }

    /**
     * Affecte un technicien à une intervention et passe le statut à ASSIGNED.
     * Seules les interventions NOUVELLE (ou ASSIGNED pour réaffecter) sont éligibles.
     *
     * @param id      identifiant de l'intervention
     * @param request contient l'identifiant du technicien choisi
     * @return l'intervention affectée (statut ASSIGNED)
     */
    @Transactional
    public InterventionDtos.InterventionResponse assign(Long id, InterventionDtos.AssignRequest request) {
        requireRole(Role.ADMIN, Role.MANAGER);
        Intervention intervention = find(id);
        // Garde-fou : interdiction d'affecter une intervention déjà en cours
        if (intervention.getStatus() != Status.NOUVELLE && intervention.getStatus() != Status.ASSIGNED) {
            throw new BusinessException(HttpStatus.CONFLICT,
                    "Seules les interventions NOUVELLE ou ASSIGNÉE peuvent être affectées (actuel: "
                            + intervention.getStatus() + ")");
        }
        Technician technician = technicianService.find(request.technicianId());
        Status from = intervention.getStatus();
        intervention.setTechnician(technician);
        intervention.setStatus(Status.ASSIGNED);
        // Audit : trace de l'affectation
        addHistory(intervention, from, Status.ASSIGNED,
                "Affectation au technicien " + technician.getUser().getFirstName()
                        + " " + technician.getUser().getLastName(), SecurityUtils.currentUser());

        // Notifications : le technicien est prévenu, le client est rassuré
        notificationService.notify(technician.getUser().getId(), NotificationType.INTERVENTION_ASSIGNED,
                "Une intervention vous a été affectée : \"" + intervention.getTitle() + "\"", intervention.getId());
        notificationService.notify(intervention.getClient().getUser().getId(), NotificationType.INTERVENTION_ASSIGNED,
                "Votre intervention \"" + intervention.getTitle() + "\" a été prise en charge", intervention.getId());
        return InterventionMapper.toResponse(intervention);
    }

    /**
     * Renvoie les suggestions automatiques de techniciens pour cette intervention
     * (voir {@link TechnicianService#suggestFor}). Droits MANAGER/ADMIN exigés.
     *
     * @param id identifiant de l'intervention
     * @return la liste des techniciens triés par score décroissant
     */
    @Transactional
    public List<InterventionDtos.SuggestionItem> suggestTechnicians(Long id) {
        requireRole(Role.ADMIN, Role.MANAGER);
        return technicianService.suggestFor(find(id));
    }

    /**
     * Applique un changement de statut après validation de la machine à états.
     * Un technicien ne peut agir que sur ses propres interventions et uniquement
     * sur les statuts autorisés (accepte, démarre, bloque, résout, refuse).
     *
     * @param id      identifiant de l'intervention
     * @param request statut cible + commentaire optionnel
     * @return l'intervention avec son nouveau statut
     */
    @Transactional
    public InterventionDtos.InterventionResponse changeStatus(Long id, InterventionDtos.StatusChangeRequest request) {
        User actor = SecurityUtils.currentUser();
        Intervention intervention = find(id);
        Status from = intervention.getStatus();
        Status to = request.newStatus();

        boolean isTechnician = actor.getRole() == Role.TECHNICIAN;
        if (isTechnician) {
            // Un technicien ne modifie que ses propres interventions
            if (intervention.getTechnician() == null
                    || !intervention.getTechnician().getUser().getId().equals(actor.getId())) {
                throw new BusinessException(HttpStatus.FORBIDDEN,
                        "Vous ne pouvez modifier que vos propres interventions");
            }
            // Le refus (ASSIGNED → NOUVELLE) est la seule transition « inverse » autorisée
            boolean refusal = from == Status.ASSIGNED && to == Status.NOUVELLE;
            if (!TECHNICIAN_STATUSES.contains(to) && !refusal) {
                throw new BusinessException(HttpStatus.FORBIDDEN,
                        "Un technicien ne peut pas passer au statut " + to);
            }
        } else {
            requireRole(Role.ADMIN, Role.MANAGER);
        }

        // Application des règles de la machine à états (NOUVELLE→ASSIGNED→…→CLOSED)
        if (!Status.canTransition(from, to)) {
            throw new BusinessException(HttpStatus.CONFLICT,
                    "Transition de statut invalide : " + from.getLabel() + " -> " + to.getLabel());
        }

        intervention.setStatus(to);
        // Gestion de la date de clôture
        if (to == Status.CLOSED && intervention.getClosedAt() == null) {
            intervention.setClosedAt(LocalDateTime.now());
        }
        if (to != Status.CLOSED) {
            intervention.setClosedAt(null);
        }

        // Historique + notifications selon la nouvelle étape
        addHistory(intervention, from, to, request.comment(), actor);
        if (isTechnician && to == Status.NOUVELLE) {
            // Refus du technicien : libère l'affectation et prévient les managers
            intervention.setTechnician(null);
            notificationService.notifyRole(Role.MANAGER, NotificationType.STATUS_CHANGED,
                    "Le technicien a refusé l'intervention \"" + intervention.getTitle() + "\"",
                    intervention.getId());
        }
        sendStatusNotifications(intervention, from, to);
        return InterventionMapper.toResponse(intervention);
    }

    /**
     * Enregistre le compte rendu et le temps réel passé par le technicien
     * (ou un manager/admin). Notifie ensuite le client.
     *
     * @param id      identifiant de l'intervention
     * @param request temps en minutes + compte rendu texte
     * @return l'intervention mise à jour
     */
    @Transactional
    public InterventionDtos.InterventionResponse submitAccount(Long id, InterventionDtos.AccountRequest request) {
        User actor = SecurityUtils.currentUser();
        Intervention intervention = find(id);
        // Seul le technicien affecté (ou un responsable) renseigne le compte rendu
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
        // Le client est informé qu'un compte rendu est disponible
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