package com.smartflow.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception métier : erreur d'état invalide, règle d'affectation violée, conflit de données…
 *
 * <p>Contrairement à {@link ResourceNotFoundException}, cette exception porte un
 * {@link HttpStatus} explicite (409 pour les conflits, 400 pour les règles violées,
 * 403 pour les permissions…). Elle est relayée par {@link GlobalExceptionHandler}
 * vers une réponse JSON structurée.</p>
 */
public class BusinessException extends RuntimeException {

    /** Code HTTP à renvoyer au client (défaut : 400 Bad Request). */
    private final HttpStatus status;

    /**
     * Crée une exception métier en HTTP 400.
     *
     * @param message message d'erreur lisible en français
     */
    public BusinessException(String message) {
        this(HttpStatus.BAD_REQUEST, message);
    }

    /**
     * Crée une exception métier avec un code HTTP explicite.
     *
     * @param status  code HTTP (par ex. {@code HttpStatus.CONFLICT})
     * @param message message d'erreur lisible en français
     */
    public BusinessException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    /**
     * Retourne le code HTTP associé à l'exception.
     *
     * @return le {@link HttpStatus} à renvoyer au client
     */
    public HttpStatus getStatus() {
        return status;
    }
}