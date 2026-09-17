package com.smartflow.entity;

/**
 * Priorités possibles d'une intervention, de la moins à la plus urgente.
 *
 * <p>Chaque constante porte un <b>libellé affichable</b> en français
 * (utilisé par le frontend et les statistiques par priorité).</p>
 *
 * <p>La priorité influe sur l'affectation automatique : une intervention
 * {@link #URGENT} ou {@link #HIGH} reçoit un bonus de score dans
 * {@code TechnicianService#suggestFor} pour être traitée plus vite.</p>
 */
public enum Priority {
    LOW("Basse"),
    MEDIUM("Moyenne"),
    HIGH("Haute"),
    URGENT("Urgente");

    /** Libellé français affiché dans l'interface et les graphiques. */
    private final String label;

    /**
     * Constructeur interne : associe chaque constante à son libellé.
     *
     * @param label libellé lisible en français
     */
    Priority(String label) {
        this.label = label;
    }

    /**
     * Retourne le libellé français de la priorité (ex. « Urgente »).
     *
     * @return le libellé affichable
     */
    public String getLabel() {
        return label;
    }
}