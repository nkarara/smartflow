package com.smartflow.entity;

/**
 * Types de notifications générées par le système.
 *
 * <p>Chaque événement métier important déclenche une notification
 * (voir {@code NotificationService#notify} et {@code InterventionService}) :</p>
 *
 * <ul>
 *   <li>{@link #NEW_INTERVENTION} : une demande client vient d'être créée.</li>
 *   <li>{@link #INTERVENTION_ASSIGNED} : un technicien vient d'être affecté.</li>
 *   <li>{@link #INTERVENTION_ACCEPTED} : le technicien a accepté l'intervention.</li>
 *   <li>{@link #STATUS_CHANGED} : changement de statut (blocage, compte rendu…).</li>
 *   <li>{@link #NEW_COMMENT} : un commentaire a été ajouté.</li>
 *   <li>{@link #NEW_ATTACHMENT} : un document/photo a été posté.</li>
 *   <li>{@link #NEW_RATING} : le client a évalué l'intervention.</li>
 *   <li>{@link #INTERVENTION_RESOLVED} : le problème est résolu.</li>
 *   <li>{@link #INTERVENTION_CLOSED} : l'intervention est clôturée.</li>
 * </ul>
 */
public enum NotificationType {
    NEW_INTERVENTION,
    INTERVENTION_ASSIGNED,
    INTERVENTION_ACCEPTED,
    STATUS_CHANGED,
    NEW_COMMENT,
    NEW_ATTACHMENT,
    NEW_RATING,
    INTERVENTION_RESOLVED,
    INTERVENTION_CLOSED
}