package com.smartflow.entity;

/**
 * Les quatre rôles de l'application.
 *
 * <ul>
 *   <li><b>ADMIN</b> : gère les utilisateurs, clients, techniciens, catégories, paramètres
 *       et les statistiques globales de l'application.</li>
 *   <li><b>MANAGER</b> : crée les interventions, consulte les demandes clients, affecte les
 *       techniciens, modifie les priorités et suit l'avancement.</li>
 *   <li><b>TECHNICIAN</b> : consulte ses interventions, accepte/refuse, change le statut,
 *       rédige le compte rendu, ajoute photos/documents et le temps passé.</li>
 *   <li><b>CLIENT</b> : crée une demande, suit son statut, ajoute des informations
 *       et évalue l'intervention une fois terminée.</li>
 * </ul>
 *
 * <p>Le nom de l'énumération sert directement d'autorité Spring Security :
 * le filtre JWT construit l'autorité {@code ROLE_<NOM>} (ex. {@code ROLE_ADMIN}),
 * utilisée par les annotations {@code @PreAuthorize("hasRole('ADMIN')")}.</p>
 */
public enum Role {
    ADMIN,
    MANAGER,
    TECHNICIAN,
    CLIENT
}