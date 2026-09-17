package com.smartflow.security;

import com.smartflow.entity.User;
import com.smartflow.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Utilitaires pour récupérer l'utilisateur courant authentifié.
 *
 * <p>À la différence de l'API Spring classique (qui expose seulement le nom),
 * ce helper retourne l'<b>entité complète</b> {@link User} grâce à
 * {@link CustomUserDetails}</b> posé dans le contexte par {@link JwtAuthenticationFilter}.</p>
 */
public final class SecurityUtils {

    /** Classe utilitaire : instanciation interdite. */
    private SecurityUtils() {
    }

    /**
     * Retourne l'utilisateur courant authentifié.
     *
     * @return l'entité {@link User} de l'utilisateur connecté
     * @throws BusinessException (401) si aucune authentification n'est présente
     */
    public static User currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        // Le principal doit être notre CustomUserDetails, sinon pas d'utilisateur fiable
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails userDetails)) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "Authentification requise");
        }
        return userDetails.getUser();
    }

    /**
     * Retourne l'identifiant de l'utilisateur courant.
     *
     * @return l'identifiant numérique ({@code id}) de l'utilisateur connecté
     */
    public static Long currentUserId() {
        return currentUser().getId();
    }
}