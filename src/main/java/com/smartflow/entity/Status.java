package com.smartflow.entity;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

/**
 * Cycle de vie d'une intervention (machine à états).
 *
 * <pre>
 * NOUVELLE ──▶ ASSIGNED ──▶ ACCEPTED ──▶ IN_PROGRESS ──▶ RESOLVED ──▶ CLOSED
 *   ▲            │▲             └──────────┴───────┐        ▲
 *   │            ││               (refus technicien)│        │
 *   └────────────┴┘                 IN_PROGRESS ◀─── BLOCKED   │
 *                                      (retour en cours)       │
 * </pre>
 *
 * <ul>
 *   <li><b>NOUVELLE</b> : créée par le client (ou par un manager).</li>
 *   <li><b>ASSIGNED</b> : un technicien a été affecté par le manager/admin.</li>
 *   <li><b>ACCEPTED</b> : le technicien a accepté l'intervention.</li>
 *   <li><b>IN_PROGRESS</b> : le travail est en cours.</li>
 *   <li><b>BLOCKED</b> : problème rencontré, l'intervention est en attente.</li>
 *   <li><b>RESOLVED</b> : problème réglé, en attente de clôture.</li>
 *   <li><b>CLOSED</b> : clôturée (état final, aucune transition possible).</li>
 * </ul>
 */
public enum Status {
    /** Nouvelle demande en attente d'affectation. */
    NOUVELLE("Nouvelle"),
    /** Un technicien a été affecté mais n'a pas encore accepté. */
    ASSIGNED("Assignée"),
    /** Le technicien a accepté l'intervention. */
    ACCEPTED("Acceptée"),
    /** L'intervention est en cours de traitement. */
    IN_PROGRESS("En cours"),
    /** L'intervention est bloquée en raison d'un imprévu. */
    BLOCKED("Bloquée"),
    /** Le problème a été résolu, en attente de clôture. */
    RESOLVED("Résolue"),
    /** L'intervention est clôturée (état terminal). */
    CLOSED("Clôturée");

    /** Libellé français affiché dans l'interface. */
    private final String label;

    /**
     * Constructeur interne : associe le libellé à chaque état.
     *
     * @param label libellé lisible en français
     */
    Status(String label) {
        this.label = label;
    }

    /**
     * Retourne le libellé français de l'état.
     *
     * @return le libellé affichable
     */
    public String getLabel() {
        return label;
    }

    /** Table de transitions autorisées : état source → états destinations possibles. */
    private static final Map<Status, Set<Status>> TRANSITIONS = new EnumMap<>(Status.class);

    /** Initialisation statique de la table de transitions. */
    static {
        // NOUVELLE : seule transition possible → ASSIGNED (affectation du technicien)
        TRANSITIONS.put(NOUVELLE, Set.of(ASSIGNED));
        // ASSIGNED : le technicien accepte (ACCEPTED) ou refuse (retour NOUVELLE)
        TRANSITIONS.put(ASSIGNED, Set.of(ACCEPTED, NOUVELLE));
        // ACCEPTED : démarrage de l'intervention
        TRANSITIONS.put(ACCEPTED, Set.of(IN_PROGRESS));
        // IN_PROGRESS : résolution ou blocage
        TRANSITIONS.put(IN_PROGRESS, Set.of(RESOLVED, BLOCKED));
        // BLOCKED : reprise en cours ou résolution directe
        TRANSITIONS.put(BLOCKED, Set.of(IN_PROGRESS, RESOLVED));
        // RESOLVED : clôture, ou retour en cours si le client signale un souci
        TRANSITIONS.put(RESOLVED, Set.of(CLOSED, IN_PROGRESS));
        // CLOSED : état terminal, aucun retour possible
        TRANSITIONS.put(CLOSED, Set.of());
    }

    /**
     * Vérifie si la transition de {@code from} vers {@code to} est autorisée.
     *
     * <p>Cas particulier : une transition de NOUVELLE vers NOUVELLE (création)
     * et de ASSIGNED vers ASSIGNED (réaffectation du technicien) sont tolérées.</p>
     *
     * @param from l'état source actuel de l'intervention
     * @param to   l'état destination demandé
     * @return {@code true} si la transition est légalement autorisée
     */
    public static boolean canTransition(Status from, Status to) {
        // Un même état est autorisé uniquement au tout début (création, réaffectation)
        if (from == to) {
            return from == NOUVELLE || from == ASSIGNED;
        }
        // Sinon, on consulte la table de transitions définie ci-dessus
        return TRANSITIONS.getOrDefault(from, Set.of()).contains(to);
    }
}