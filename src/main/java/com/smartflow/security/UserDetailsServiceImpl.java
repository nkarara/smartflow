package com.smartflow.security;

import com.smartflow.entity.User;
import com.smartflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Pont entre la table {@code users} et Spring Security :
 * charge un utilisateur par son email pour l'authentification.
 *
 * <p>Cette classe est utilisée par :
 * <ul>
 *   <li>{@code DaoAuthenticationProvider} — vérification du mot de passe à la connexion ;</li>
 *   <li>{@link JwtAuthenticationFilter} — reconstitution de l'utilisateur depuis le token JWT.</li>
 * </ul>
 * </p>
 */
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    /** Dépôt d'accès à la table des utilisateurs. */
    private final UserRepository userRepository;

    /**
     * Charge un utilisateur par son adresse email (insensible à la casse).
     *
     * @param email l'adresse email saisie à la connexion ou extraite du JWT
     * @return les détails de sécurité encapsulant l'utilisateur
     * @throws UsernameNotFoundException si l'utilisateur n'existe pas ou est désactivé
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // Recherche par email (trim + ignoreCase), sinon erreur « identifiants invalides »
        User user = userRepository.findByEmailIgnoreCase(email.trim())
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur introuvable: " + email));
        // Un compte désactivé ne peut pas se connecter
        if (!user.isEnabled()) {
            throw new UsernameNotFoundException("Compte désactivé");
        }
        return new CustomUserDetails(user);
    }
}