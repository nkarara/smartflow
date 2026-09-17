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
 * Refresh token permettant de renouveler l'access token sans redemander le
 * mot de passe (mécanisme d'« authentification sans friction »).
 *
 * <p>Le jeton est stocké en base (table {@code refresh_tokens}), lié à un
 * utilisateur, avec une expiration et un drapeau {@link #revoked} pour la
 * révocation (déconnexion) et la rotation (chaque refresh invalide l'ancien).</p>
 */
@Entity
@Table(name = "refresh_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken {

    /** Identifiant technique auto-généré. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Jeton opaque unique (UUID) utilisé comme secret d'échange. */
    @Column(nullable = false, unique = true, length = 190)
    private String token;

    /** Utilisateur propriétaire du jeton. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** Date d'expiration ; au-delà, l'utilisateur doit se reconnecter. */
    @Column(nullable = false)
    private LocalDateTime expiresAt;

    /** Jeton révoqué (déconnexion ou rotation) : refusé par {@link #isValid()}. */
    @Builder.Default
    @Column(nullable = false)
    private boolean revoked = false;

    /** Date de création du jeton (automatique, non modifiable). */
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Positionne {@link #createdAt} avant l'insertion en base.
     * Indispensable car la colonne est {@code NOT NULL}.
     */
    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    /**
     * Un jeton est utilisable s'il n'est ni révoqué ni expiré.
     *
     * @return {@code true} si le jeton peut servir à rafraîchir la session
     */
    public boolean isValid() {
        return !revoked && expiresAt.isAfter(LocalDateTime.now());
    }
}