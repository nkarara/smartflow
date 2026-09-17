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
 * Commentaire ajouté sur une intervention par un participant
 * (client, technicien, manager ou admin).
 *
 * <p>Chaque commentaire est rattaché à une {@link Intervention} et à son
 * auteur ({@link User}). Sa création déclenche une notification
 * {@link NotificationType#NEW_COMMENT} vers les autres participants.</p>
 */
@Entity
@Table(name = "comments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Comment {

    /** Identifiant technique auto-généré. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Intervention concernée (relation N-1 obligatoire). */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "intervention_id", nullable = false)
    private Intervention intervention;

    /** Utilisateur ayant rédigé le commentaire. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    /** Contenu du commentaire (texte libre). */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

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