package com.smartflow.exception;

/**
 * Exception levée lorsqu'une ressource demandée n'existe pas en base.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String resource, Object id) {
        super("Ressource " + resource + " introuvable (id: " + id + ")");
    }

    public ResourceNotFoundException(String message) {
        super(message);
    }
}