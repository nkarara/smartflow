package com.smartflow.service;

import com.smartflow.dto.AppSettingDtos;
import com.smartflow.entity.AppSetting;
import com.smartflow.exception.ResourceNotFoundException;
import com.smartflow.repository.AppSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Gestion des paramètres de l'application (rôle ADMIN uniquement).
 *
 * <p>Permet à l'administrateur de consulter et de modifier les valeurs de
 * configuration stockées en base (coordonnées, règles SLA, limites techniques…).</p>
 */
@Service
@RequiredArgsConstructor
public class SettingsService {

    /** Dépôt d'accès aux paramètres applicatifs. */
    private final AppSettingRepository appSettingRepository;

    /**
     * Liste tous les paramètres triés par clé.
     *
     * @return les paramètres au format DTO
     */
    @Transactional(readOnly = true)
    public List<AppSettingDtos.SettingResponse> listAll() {
        return appSettingRepository.findAllByOrderByKeyAsc().stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Retourne un paramètre par sa clé.
     *
     * @param key nom unique du paramètre
     * @return le paramètre au format DTO
     * @throws ResourceNotFoundException si la clé n'existe pas
     */
    @Transactional(readOnly = true)
    public AppSettingDtos.SettingResponse get(String key) {
        return toResponse(find(key));
    }

    /**
     * Met à jour la valeur d'un paramètre existant.
     * La création de nouvelles clés est volontairement non autorisée
     * (les paramètres sont définis par le système, l'admin n'en change que la valeur).
     *
     * @param key     nom unique du paramètre
     * @param request nouvelle valeur
     * @return le paramètre mis à jour
     */
    @Transactional
    public AppSettingDtos.SettingResponse update(String key, AppSettingDtos.UpdateSettingRequest request) {
        AppSetting setting = find(key);
        setting.setValue(request.value().trim());
        appSettingRepository.save(setting);
        return toResponse(setting);
    }

    /**
     * Conversion interne entité → DTO.
     *
     * @param setting l'entité persistée
     * @return la représentation exposée par l'API
     */
    private AppSettingDtos.SettingResponse toResponse(AppSetting setting) {
        return new AppSettingDtos.SettingResponse(
                setting.getKey(),
                setting.getValue(),
                setting.getDescription(),
                setting.getCategory()
        );
    }

    /**
     * Recherche une entité par sa clé.
     *
     * @param key la clé recherchée
     * @return l'entité trouvée
     */
    private AppSetting find(String key) {
        return appSettingRepository.findByKey(key)
                .orElseThrow(() -> new ResourceNotFoundException("Paramètre (clé: " + key + ")"));
    }
}