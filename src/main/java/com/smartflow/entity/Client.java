package com.smartflow.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Profil métier « client » lié à un {@link User} de rôle {@link Role#CLIENT}.
 *
 * <p>Relation 1-1 avec {@code users</code>, une liste 1-N d'interventions,
 * et des informations de société/entreprise pour le suivi RH et la facturation.</p>
 */
@Entity
@Table(name = "clients")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Client {

    /** Identifiant technique auto-généré. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Compte utilisateur associé (un client = un utilisateur). */
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", unique = true)
    private User user;

    /** Nom de l'entreprise du client. */
    @Column(length = 190)
    private String companyName;

    /** Adresse physique (rue) du client. */
    @Column(length = 255)
    private String address;

    /** Ville du client (utilisée pour l'affectation par localisation). */
    @Column(length = 120)
    private String city;

    /** Numéro SIRET de l'entreprise (facultatif). */
    @Column(length = 30)
    private String siret;

    /** Toutes les interventions de ce client (colonne inverse de la relation). */
    @Builder.Default
    @OneToMany(mappedBy = "client")
    private List<Intervention> interventions = new ArrayList<>();
}