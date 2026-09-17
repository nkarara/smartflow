package com.smartflow.exception;

/**
 * Exception levée lorsqu'une ressource demandée n'existe pas en base.
 *
 * <p>Traduite par {@link GlobalExceptionHandler} en HTTP <b>404 Not Found</b>
 * avec un message du type « Ressource {ressource} introuvable (id: {id}) ».</p>
 */
public class ResourceNotFoundException extends RuntimeException {

    /**
     * Construit l'exception avec le type de ressource et l'identifiant recherché.
     *
     * @param resource nom lisible de la ressource (ex. « Intervention »)
     * @param id       identifiant non trouvé
     */
    public ResourceNotFoundException(String resource, Object id) {
        super("Ressource " + resource + " introuvable (id: " + id + ")");
    }

    /**
     * Construit l'exception avec un message libre.
     *
     * @param message message d'erreur lisible en français
     */
    public ResourceNotFoundException(String message) {
        super(message);
    }
}