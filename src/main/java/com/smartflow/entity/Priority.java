package com.smartflow.entity;

/**
 * Priorités d'une intervention.
 */
public enum Priority {
    LOW("Basse"),
    MEDIUM("Moyenne"),
    HIGH("Haute"),
    URGENT("Urgente");

    private final String label;

    Priority(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}