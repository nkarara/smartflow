package com.smartflow.entity;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

/**
 * Cycle de vie d'une intervention.
 * NOUVELLE -> ASSIGNED -> ACCEPTED -> IN_PROGRESS -> RESOLVED -> CLOSED
 * IN_PROGRESS <-> BLOCKED
 */
public enum Status {
    NOUVELLE("Nouvelle"),
    ASSIGNED("Assignée"),
    ACCEPTED("Acceptée"),
    IN_PROGRESS("En cours"),
    BLOCKED("Bloquée"),
    RESOLVED("Résolue"),
    CLOSED("Clôturée");

    private final String label;

    Status(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    private static final Map<Status, Set<Status>> TRANSITIONS = new EnumMap<>(Status.class);

    static {
        TRANSITIONS.put(NOUVELLE, Set.of(ASSIGNED));
        TRANSITIONS.put(ASSIGNED, Set.of(ACCEPTED, NOUVELLE));
        TRANSITIONS.put(ACCEPTED, Set.of(IN_PROGRESS));
        TRANSITIONS.put(IN_PROGRESS, Set.of(RESOLVED, BLOCKED));
        TRANSITIONS.put(BLOCKED, Set.of(IN_PROGRESS, RESOLVED));
        TRANSITIONS.put(RESOLVED, Set.of(CLOSED, IN_PROGRESS));
        TRANSITIONS.put(CLOSED, Set.of());
    }

    /**
     * Vérifie si la transition de {@code from} vers {@code to} est autorisée,
     * avec le cas particulier NOUVELLE -> NOUVELLE (création) et les réaffectations ASSIGNED -> ASSIGNED.
     */
    public static boolean canTransition(Status from, Status to) {
        if (from == to) {
            return from == NOUVELLE || from == ASSIGNED;
        }
        return TRANSITIONS.getOrDefault(from, Set.of()).contains(to);
    }
}