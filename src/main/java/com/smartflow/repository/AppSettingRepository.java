package com.smartflow.repository;

import com.smartflow.entity.AppSetting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Dépôt d'accès aux paramètres applicatifs.
 */
public interface AppSettingRepository extends JpaRepository<AppSetting, Long> {

    /** Recherche un paramètre par son nom de clé. */
    Optional<AppSetting> findByKey(String key);

    /** Liste tous les paramètres triés par clé (lecture dans la page admin). */
    List<AppSetting> findAllByOrderByKeyAsc();

    /** Indique si une clé existe déjà (unicité). */
    boolean existsByKey(String key);
}