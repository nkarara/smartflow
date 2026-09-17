package com.smartflow.entity;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entité centrale du domaine : une demande d'intervention.
 *
 * <p>Une intervention relie un {@link Client}, un {@link Technician} (affecté), une
 * {@link Category}, un niveau de {@link Priority} et suit un cycle de vie
 * ({@link Status}, voir {@link #canTransition}).</p>
 *
 * <p>Les collections liées (commentaires, pièces jointes, historique) utilisent
 * {@code cascade = ALL} et {@code orphanRemoval = true} : leur cycle de vie
 * est entièrement dépendant de l'intervention.</p>
 */
@Entity
@Table(name = "interventions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Intervention {

    /** Identifiant technique auto-généré (référencé dans les notifications). */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Titre court et explicite de l'intervention (ex. « Ordinateur en panne »). */
    @Column(nullable = false, length = 190)
    private String title;

    /** Description détaillée du problème rapporté par le client. */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    /** Client demandeur (obligatoire). */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    /** Technicien affecté (null tant que l'intervention n'est pas assignée). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "technician_id")
    private Technician technician;

    /** Catégorie métier de l'intervention (obligatoire). */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    /** Priorité de traitement (défaut : MEDIUM). */
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Priority priority = Priority.MEDIUM;

    /** Localisation du site d'intervention (utilisée par l'affectation automatique). */
    @Column(length = 255)
    private String location;

    /** Date/heure de création (positionnée automatiquement, non modifiable). */
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** Date prévue/souhaitée par le client (optionnelle). */
    private LocalDateTime plannedDate;

    /** État courant dans la machine à états (défaut : NOUVELLE). */
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.NOUVELLE;

    /** Temps estimé en minutes (proposé par le manager ou l'IA). */
    private Integer estimatedTimeMinutes;

    /** Temps réellement passé en minutes (saisi par le technicien). */
    private Integer actualTimeMinutes;

    /** Compte rendu rédigé par le technicien en fin d'intervention. */
    @Column(columnDefinition = "TEXT")
    private String report;

    /** Date de clôture effective (positionnée lors du passage à CLOSED). */
    private LocalDateTime closedAt;

    /** Liste des commentaires (1-N, suppression en cascade). */
    @Builder.Default
    @OneToMany(mappedBy = "intervention", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Comment> comments = new ArrayList<>();

    /** Liste des pièces jointes (photos/documents, 1-N, suppression en cascade). */
    @Builder.Default
    @OneToMany(mappedBy = "intervention", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Attachment> attachments = new ArrayList<>();

    /** Historique des changements de statut (1-N, suppression en cascade). */
    @Builder.Default
    @OneToMany(mappedBy = "intervention", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InterventionHistory> history = new ArrayList<>();

    /**
     * Callback JPA exécuté avant l'insertion :
     * initialise {@link #createdAt} et applique les valeurs par défaut
     * {@link #status} = NOUVELLE et {@link #priority} = MEDIUM.
     */
    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == null) {
            status = Status.NOUVELLE;
        }
        if (priority == null) {
            priority = Priority.MEDIUM;
        }
    }
}