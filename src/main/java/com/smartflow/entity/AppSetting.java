package com.smartflow.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Paramètre applicatif éditable par l'administrateur (table {@code app_settings}).
 *
 * <p>Paires clé/valeur typées en texte, groupées par catégorie :
 * <ul>
 *   <li>{@code company.name}, {@code support.email} → informations affichées ;</li>
 *   <li>{@code sla.*}, {@code notifications.enabled} → règles fonctionnelles ;</li>
 *   <li>{@code upload.maxSizeMB} → limites techniques.</li>
 * </ul>
 * L'API de gestion est réservée au rôle {@link Role#ADMIN}.</p>
 */
@Entity
@Table(name = "app_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppSetting {

    /** Identifiant technique auto-généré. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Nom unique du paramètre (ex. {@code support.email}). Colonne mappée en {@code setting_key} car {@code key} est un mot réservé SQL. */
    @Column(name = "setting_key", nullable = false, unique = true, length = 190)
    private String key;

    /** Valeur actuelle du paramètre. Colonne mappée en {@code setting_value} (`value` est réservé en SQL). */
    @Column(name = "setting_value", nullable = false, length = 1000)
    private String value;

    /** Description lisible indiquant l'utilité du paramètre. */
    @Column(length = 500)
    private String description;

    /** Catégorie d'affichage (ex. « Général », « Notifications », « SLA »). */
    @Column(length = 60)
    private String category;
}