package com.smartflow.security;

import com.smartflow.entity.User;
import com.smartflow.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Utilitaires pour récupérer l'utilisateur courant authentifié.
 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static User currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails userDetails)) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "Authentification requise");
        }
        return userDetails.getUser();
    }

    public static Long currentUserId() {
        return currentUser().getId();
    }
}