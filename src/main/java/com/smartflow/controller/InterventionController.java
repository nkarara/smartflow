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
 * Cycle de vie des interventions : création, modification, affectation, statut, compte rendu, historique.
 * La visibilité et les droits sont vérifiés selon le rôle dans la couche service.
 */
@RestController
@RequestMapping("/api/interventions")
@RequiredArgsConstructor
public class InterventionController {

    private final InterventionService interventionService;

    @GetMapping
    public List<InterventionDtos.InterventionResponse> list() {
        return interventionService.listAllForCurrentUser();
    }

    @GetMapping("/{id}")
    public InterventionDtos.InterventionResponse get(@PathVariable Long id) {
        return interventionService.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InterventionDtos.InterventionResponse create(@Valid @RequestBody InterventionDtos.CreateRequest request) {
        return interventionService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public InterventionDtos.InterventionResponse update(@PathVariable Long id,
                                                       @Valid @RequestBody InterventionDtos.UpdateRequest request) {
        return interventionService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        interventionService.delete(id);
    }

    @PutMapping("/{id}/assign")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public InterventionDtos.InterventionResponse assign(@PathVariable Long id,
                                                       @Valid @RequestBody InterventionDtos.AssignRequest request) {
        return interventionService.assign(id, request);
    }

    @GetMapping("/{id}/assign/suggestions")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public List<InterventionDtos.SuggestionItem> suggestions(@PathVariable Long id) {
        return interventionService.suggestTechnicians(id);
    }

    @PutMapping("/{id}/status")
    public InterventionDtos.InterventionResponse changeStatus(@PathVariable Long id,
                                                             @Valid @RequestBody InterventionDtos.StatusChangeRequest request) {
        return interventionService.changeStatus(id, request);
    }

    @PutMapping("/{id}/account")
    public InterventionDtos.InterventionResponse submitAccount(@PathVariable Long id,
                                                              @Valid @RequestBody InterventionDtos.AccountRequest request) {
        return interventionService.submitAccount(id, request);
    }

    @GetMapping("/{id}/history")
    public List<InterventionDtos.HistoryResponse> history(@PathVariable Long id) {
        return interventionService.getHistory(id);
    }
}