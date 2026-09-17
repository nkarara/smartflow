package com.smartflow.controller;

import com.smartflow.dto.InterventionDtos;
import com.smartflow.service.InterventionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Cycle de vie des interventions : création, modification, affectation, statut,
 * compte rendu, historique. La visibilité et les droits sont vérifiés selon le
 * rôle dans la couche service ({@link InterventionService}).
 */
@RestController
@RequestMapping("/api/interventions")
@RequiredArgsConstructor
public class InterventionController {

    /** Service métier des interventions (accès contrôlé par rôle). */
    private final InterventionService interventionService;

    /** Liste des interventions visibles par l'utilisateur courant. */
    @GetMapping
    public List<InterventionDtos.InterventionResponse> list() {
        return interventionService.listAllForCurrentUser();
    }

    /** Détail d'une intervention (accès réservé aux participants/autorités). */
    @GetMapping("/{id}")
    public InterventionDtos.InterventionResponse get(@PathVariable Long id) {
        return interventionService.get(id);
    }

    /** Crée une demande (statut NOUVELLE) — client ou manager/admin. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InterventionDtos.InterventionResponse create(@Valid @RequestBody InterventionDtos.CreateRequest request) {
        return interventionService.create(request);
    }

    /** Modifie une intervention — droits MANAGER/ADMIN. */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public InterventionDtos.InterventionResponse update(@PathVariable Long id,
                                                       @Valid @RequestBody InterventionDtos.UpdateRequest request) {
        return interventionService.update(id, request);
    }

    /** Supprime définitivement une intervention — droits ADMIN. */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        interventionService.delete(id);
    }

    /** Affecte un technicien à l'intervention — droits MANAGER/ADMIN. */
    @PutMapping("/{id}/assign")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public InterventionDtos.InterventionResponse assign(@PathVariable Long id,
                                                       @Valid @RequestBody InterventionDtos.AssignRequest request) {
        return interventionService.assign(id, request);
    }

    /** Suggestions automatiques de techniciens pour cette intervention. */
    @GetMapping("/{id}/assign/suggestions")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public List<InterventionDtos.SuggestionItem> suggestions(@PathVariable Long id) {
        return interventionService.suggestTechnicians(id);
    }

    /** Change le statut (l'acceptation/le refus du technicien passent aussi par ici). */
    @PutMapping("/{id}/status")
    public InterventionDtos.InterventionResponse changeStatus(@PathVariable Long id,
                                                             @Valid @RequestBody InterventionDtos.StatusChangeRequest request) {
        return interventionService.changeStatus(id, request);
    }

    /** Enregistre le compte rendu + temps réel (technicien ou responsable). */
    @PutMapping("/{id}/account")
    public InterventionDtos.InterventionResponse submitAccount(@PathVariable Long id,
                                                              @Valid @RequestBody InterventionDtos.AccountRequest request) {
        return interventionService.submitAccount(id, request);
    }

    /** Historique d'audit des changements de statut. */
    @GetMapping("/{id}/history")
    public List<InterventionDtos.HistoryResponse> history(@PathVariable Long id) {
        return interventionService.getHistory(id);
    }
}