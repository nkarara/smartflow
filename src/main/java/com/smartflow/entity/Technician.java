package com.smartflow.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Profil métier « technicien » lié à un {@link User} de rôle {@link Role#TECHNICIAN}.
 *
 * <p>Contient les informations utilisées par l'<b>affectation automatique</b>
 * ({@code TechnicianService#suggestFor}) : disponibilité, localisation et
 * compétences techniques (relation N-N avec {@link Skill}).</p>
 */
@Entity
@Table(name = "technicians")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Technician {

    /** Identifiant technique auto-généré. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Compte utilisateur associé (un technicien = un utilisateur). */
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", unique = true)
    private User user;

    /** Spécialité principale affichée (ex. « Génie informatique »). */
    @Column(length = 190)
    private String specialty;

    /** Ville de rattachement — utilisée pour rapprocher le technicien de l'intervention. */
    @Column(length = 120)
    private String location;

    /** Téléphone direct du technicien. */
    @Column(length = 30)
    private String phone;

    /** Disponibilité courante ; un technicien indisponible est pénalisé dans les suggestions. */
    @Builder.Default
    @Column(nullable = false)
    private boolean available = true;

    /** Date d'embauche du technicien. */
    private LocalDate hireDate;

    /**
     * Compétences du technicien (relation N-N).
     * L'entité de jointure {@code technician_skills} est gérée automatiquement par Hibernate.
     * Le chargement est EAGER + initialisé par {@code @Builder.Default} pour éviter le null.
     */
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "technician_skills",
            joinColumns = @JoinColumn(name = "technician_id"),
            inverseJoinColumns = @JoinColumn(name = "skill_id")
    )
    @Builder.Default
    private Set<Skill> skills = new LinkedHashSet<>();
}