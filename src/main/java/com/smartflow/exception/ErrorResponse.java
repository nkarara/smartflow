package com.smartflow.exception;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Structure JSON normalisée des erreurs renvoyées par l'API.
 *
 * <p>Exemple de corps de réponse :</p>
 * <pre>
 * {
 *   "timestamp":   "2026-09-12T10:00:00",
 *   "status":      409,
 *   "error":       "Conflict",
 *   "message":     "Transition de statut invalide",
 *   "path":        "/api/interventions/10/status",
 *   "fieldErrors": { "title": "Le titre est obligatoire" }
 * }
 * </pre>
 *
 * @param timestamp   date/heure de l'erreur
 * @param status      code HTTP
 * @param error       intitulé technique du code HTTP
 * @param message     message lisible en français
 * @param path        URL qui a déclenché l'erreur
 * @param fieldErrors erreurs de validation par champ (nullable)
 */
public record ErrorResponse(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        String path,
        Map<String, String> fieldErrors
) {
    /**
     * Fabrique une réponse d'erreur simple (sans erreurs de champ).
     *
     * @param status  code HTTP
     * @param error   intitulé technique
     * @param message message lisible
     * @param path    URL concernée
     * @return l'instance {@link ErrorResponse}
     */
    public static ErrorResponse of(int status, String error, String message, String path) {
        return new ErrorResponse(LocalDateTime.now(), status, error, message, path, null);
    }

    /**
     * Fabrique une réponse d'erreur avec erreurs de validation par champ.
     *
     * @param status      code HTTP
     * @param error       intitulé technique
     * @param message     message global lisible
     * @param path        URL concernée
     * @param fieldErrors map champ → message d'erreur
     * @return l'instance {@link ErrorResponse}
     */
    public static ErrorResponse of(int status, String error, String message, String path, Map<String, String> fieldErrors) {
        return new ErrorResponse(LocalDateTime.now(), status, error, message, path, fieldErrors);
    }
}