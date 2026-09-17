package com.smartflow.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Compte utilisateur de l'application (table {@code users}).
 *
 * <p>Un utilisateur est partagé par tous les rôles :
 * <ul>
 *   <li>{@link Role#CLIENT} → un profil {@link Client} lui est associé (OneToOne) ;</li>
 *   <li>{@link Role#TECHNICIAN} → un profil {@link Technician} lui est associé (OneToOne) ;</li>
 *   <li>{@link Role#ADMIN} / {@link Role#MANAGER} → aucun profil métier supplémentaire.</li>
 * </ul>
 * </p>
 *
 * <p>Le mot de passe est stocké <b>haché</b> (BCrypt) — jamais en clair.</p>
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    /** Identifiant technique auto-généré par la base. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Adresse email, unique dans la base et utilisée comme identifiant de connexion. */
    @Column(nullable = false, unique = true, length = 190)
    private String email;

    /** Mot de passe haché (BCrypt) — jamais renvoyé par l'API. */
    @Column(nullable = false)
    private String password;

    /** Prénom de l'utilisateur. */
    @Column(nullable = false, length = 120)
    private String firstName;

    /** Nom de famille de l'utilisateur. */
    @Column(nullable = false, length = 120)
    private String lastName;

    /** Numéro de téléphone (facultatif). */
    @Column(length = 30)
    private String phone;

    /** Rôle attribué : détermine toutes les autorisations (ROLE_xxx). */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    /** Compte activé/désactivé ; un compte désactivé ne peut plus se connecter. */
    @Builder.Default
    @Column(nullable = false)
    private boolean enabled = true;

    /** Date de création du compte (positionnée automatiquement, non modifiable). */
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Callback JPA exécuté AVANT l'insertion ({@code persist}) :
     * initialise {@link #createdAt} si elle n'est pas encore définie.
     */
    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}