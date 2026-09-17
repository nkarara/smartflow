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
 * Notification destinée à un utilisateur lorsqu'un événement important
 * survient (nouvelle demande, affectation, acceptation, commentaire…).
 *
 * <p>Le type {@link NotificationType} décrit l'événement et
 * {@link #relatedInterventionId} permet de naviguer vers l'intervention
 * concernée depuis l'interface.</p>
 */
@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    /** Identifiant technique auto-généré. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Utilisateur destinataire de la notification. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** Type d'événement qui a déclenché la notification. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private NotificationType type;

    /** Message lisible en français (ex. « Une intervention vous a été affectée »). */
    @Column(nullable = false, length = 500)
    private String message;

    /** Identifiant de l'intervention associée (null si non liée). */
    private Long relatedInterventionId;

    /** Indique si la notification a été lue (utilisé pour le badge non-lu). */
    @Builder.Default
    @Column(nullable = false)
    private boolean read = false;

    /** Date/heure de création (automatique, non modifiable). */
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