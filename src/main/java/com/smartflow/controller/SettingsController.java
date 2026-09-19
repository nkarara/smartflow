package com.smartflow.controller;

import com.smartflow.dto.AppSettingDtos;
import com.smartflow.service.SettingsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Gestion des paramètres de l'application — réservé à l'ADMINISTRATEUR.
 */
@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class SettingsController {

    /** Service métier de gestion des paramètres. */
    private final SettingsService settingsService;

    /** Liste tous les paramètres configurables (page admin). */
    @GetMapping
    public List<AppSettingDtos.SettingResponse> listAll() {
        return settingsService.listAll();
    }

    /** Détail d'un paramètre par sa clé. */
    @GetMapping("/{key}")
    public AppSettingDtos.SettingResponse get(@PathVariable String key) {
        return settingsService.get(key);
    }

    /** Met à jour la valeur d'un paramètre existant. */
    @PutMapping("/{key}")
    public AppSettingDtos.SettingResponse update(@PathVariable String key,
                                                 @Valid @RequestBody AppSettingDtos.UpdateSettingRequest request) {
        return settingsService.update(key, request);
    }
}