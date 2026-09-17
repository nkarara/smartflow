package com.smartflow.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Évaluation d'une intervention par le client (note de 1 à 5 + commentaire).
 *
 * <p>Un client ne peut évaluer qu'une fois son intervention : la colonne
 * {@code intervention_id} est unique. Une nouvelle évaluation écrase la précédente
 * (voir {@code RatingService#rate}).</p>
 */
@Entity
@Table(name = "ratings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Rating {

    /** Identifiant technique auto-généré. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Intervention évaluée (une seule évaluation par intervention : contrainte unique). */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "intervention_id", nullable = false, unique = true)
    private Intervention intervention;

    /** Note attribuée, entre 1 et 5 (validée par Bean Validation). */
    @Column(nullable = false)
    private int score;

    /** Commentaire libre optionnel accompagnant la note. */
    @Column(length = 1000)
    private String comment;

    /** Date/heure de l'évaluation (automatique, non modifiable). */
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** Positionne {@link #createdAt} avant l'insertion en base. */
    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}