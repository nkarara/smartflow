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
 * Compétence technique d'un technicien (ex. « Réseau », « Impression »).
 *
 * <p>Les compétences servent à l'affectation intelligente : le moteur de
 * suggestion vérifie si le nom d'une compétence apparaît dans le texte
 * (titre + description) de l'intervention pour attribuer un bonus de score.</p>
 */
@Entity
@Table(name = "skills")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Skill {

    /** Identifiant technique auto-généré. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Nom de la compétence, unique (ex. « Réseau »). */
    @Column(nullable = false, unique = true, length = 120)
    private String name;

    /** Description optionnelle de la compétence. */
    @Column(length = 190)
    private String description;
}