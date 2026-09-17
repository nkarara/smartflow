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
 * Catégorie d'intervention (ex. « Réseau », « Imprimante », « Matériel informatique »).
 *
 * <p>Les catégories sont utilisées par :
 * <ul>
 *   <li>le client lors de la création d'une demande ;</li>
 *   <li>l'IA pour classer automatiquement les descriptions ;</li>
 *   <li>les statistiques « interventions par catégorie » du tableau de bord.</li>
 * </ul>
 * </p>
 */
@Entity
@Table(name = "categories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Category {

    /** Identifiant technique auto-généré. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Nom unique de la catégorie. */
    @Column(nullable = false, unique = true, length = 120)
    private String name;

    /** Description facultative de la catégorie. */
    @Column(length = 500)
    private String description;

    /** Couleur hexadécimale (affichée dans l'interface). */
    @Column(length = 30)
    private String color;

    /** Emoji icône affiché dans l'interface (ex. 🌐). */
    @Column(length = 30)
    private String icon;
}