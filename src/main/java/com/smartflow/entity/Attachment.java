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
 * Pièce jointe (photo, document…) rattachée à une intervention.
 *
 * <p>Le fichier binaire est stocké sur le <b>disque serveur</b> (répertoire
 * {@code smartflow.upload-dir}/intervention-{id}) ; cette entité n'en conserve
 * que les métadonnées et le chemin local pour le téléchargement.</p>
 */
@Entity
@Table(name = "attachments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Attachment {

    /** Identifiant technique auto-généré. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Intervention à laquelle le document est rattaché. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "intervention_id", nullable = false)
    private Intervention intervention;

    /** Utilisateur ayant téléversé le document. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "uploaded_by_id", nullable = false)
    private User uploadedBy;

    /** Nom d'origine du fichier (affiché lors du téléchargement). */
    @Column(nullable = false, length = 255)
    private String fileName;

    /** Type MIME du fichier (ex. image/png, application/pdf). */
    @Column(length = 120)
    private String contentType;

    /** Taille du fichier en octets. */
    private long size;

    /** Chemin absolu du fichier sur le serveur. */
    @Column(nullable = false, length = 500)
    private String filePath;

    /** Date/heure du téléversement (automatique, non modifiable). */
    @Column(nullable = false, updatable = false)
    private LocalDateTime uploadedAt;

    /** Positionne {@link #uploadedAt} avant l'insertion en base. */
    @PrePersist
    void onCreate() {
        if (uploadedAt == null) {
            uploadedAt = LocalDateTime.now();
        }
    }
}