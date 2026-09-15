package com.smartflow.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Règles de transition de la machine à états des interventions.
 */
class StatusTest {

    @Test
    void validTransitions() {
        assertTrue(Status.canTransition(Status.NOUVELLE, Status.ASSIGNED));
        assertTrue(Status.canTransition(Status.ASSIGNED, Status.ACCEPTED));
        assertTrue(Status.canTransition(Status.ASSIGNED, Status.NOUVELLE)); // refus du technicien
        assertTrue(Status.canTransition(Status.ACCEPTED, Status.IN_PROGRESS));
        assertTrue(Status.canTransition(Status.IN_PROGRESS, Status.RESOLVED));
        assertTrue(Status.canTransition(Status.IN_PROGRESS, Status.BLOCKED));
        assertTrue(Status.canTransition(Status.BLOCKED, Status.IN_PROGRESS));
        assertTrue(Status.canTransition(Status.BLOCKED, Status.RESOLVED));
        assertTrue(Status.canTransition(Status.RESOLVED, Status.CLOSED));
    }

    @Test
    void invalidTransitions() {
        assertFalse(Status.canTransition(Status.NOUVELLE, Status.RESOLVED));
        assertFalse(Status.canTransition(Status.NOUVELLE, Status.IN_PROGRESS));
        assertFalse(Status.canTransition(Status.ACCEPTED, Status.CLOSED));
        assertFalse(Status.canTransition(Status.IN_PROGRESS, Status.CLOSED));
        assertFalse(Status.canTransition(Status.CLOSED, Status.IN_PROGRESS));
        assertFalse(Status.canTransition(Status.CLOSED, Status.CLOSED));
    }
}