package com.smartflow.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
 * Trace complète des changements de statut d'une intervention.
 *
 * <p>Chaque événement important du cycle de vie est archivé ici
 * (qui, de quel état vers quel état, quand, avec quel commentaire).
 * Cela fournit l'<b>historique d'audit</b> affiché en bas de la page de détail.</p>
 */
@Entity
@Table(name = "intervention_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InterventionHistory {

    /** Identifiant technique auto-généré. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Intervention auditée (relation N-1). */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "intervention_id", nullable = false)
    private Intervention intervention;

    /** Utilisateur à l'origine du changement (null pour les événements système). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "changed_by_id")
    private User changedBy;

    /** État avant le changement (null à la création). */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Status fromStatus;

    /** État après le changement (jamais null). */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status toStatus;

    /** Motif / commentaire associé au changement (optionnel). */
    @Column(length = 1000)
    private String comment;

    /** Date/heure du changement (automatique, non modifiable). */
    @Column(nullable = false, updatable = false)
    private LocalDateTime changedAt;

    /** Positionne {@link #changedAt} avant l'insertion en base. */
    @PrePersist
    void onCreate() {
        if (changedAt == null) {
            changedAt = LocalDateTime.now();
        }
    }
}