package com.smartflow.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception métier: transition de statut invalide, conflit, règle d'affectation, etc.
 */
public class BusinessException extends RuntimeException {

    private final HttpStatus status;

    public BusinessException(String message) {
        this(HttpStatus.BAD_REQUEST, message);
    }

    public BusinessException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}