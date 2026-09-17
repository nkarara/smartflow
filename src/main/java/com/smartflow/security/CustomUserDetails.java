package com.smartflow.security;

import com.smartflow.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Adaptateur entre notre entité {@link User} et l'interface {@link UserDetails}
 * attendue par Spring Security.
 *
 * <p>Il permet à Spring Security (DaoAuthenticationProvider, filtre JWT) de
 * <b>combiner</b> notre modèle de données (email, mot de passe haché, rôle, compte actif)
 * avec les mécanismes standards d'authentification. L'utilisateur original est
 * conservé dans {@link #user} pour que les services puissent le récupérer facilement
 * via {@link SecurityUtils#currentUser()}.</p>
 */
public class CustomUserDetails implements UserDetails {

    /** L'utilisateur applicatif complet (détaché des besoins de Spring Security). */
    private final User user;

    /** Autorités attribuées : une seule par rôle, préfixée {@code ROLE_} (ex. {@code ROLE_ADMIN}). */
    private final List<GrantedAuthority> authorities;

    /**
     * Construit les détails de sécurité à partir de l'utilisateur applicatif.
     *
     * @param user l'entité {@link User} chargée depuis la base
     */
    public CustomUserDetails(User user) {
        this.user = user;
        this.authorities = List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
    }

    /** Retourne l'entité utilisateur applicative complète. */
    public User getUser() {
        return user;
    }

    /** Retourne la liste des autorités (utilisée par {@code hasRole(...)}). */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    /** Retourne le mot de passe haché (BCrypt) pour la vérification à la connexion. */
    @Override
    public String getPassword() {
        return user.getPassword();
    }

    /** Retourne l'identifiant de connexion : l'adresse email. */
    @Override
    public String getUsername() {
        return user.getEmail();
    }

    /** Le compte n'est jamais expiré dans ce modèle. */
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    /** Le compte n'est jamais verrouillé dans ce modèle. */
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    /** Les identifiants ne sont jamais expirés dans ce modèle. */
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /** Seul le compte <b>activé</b> peut se connecter. */
    @Override
    public boolean isEnabled() {
        return user.isEnabled();
    }
}