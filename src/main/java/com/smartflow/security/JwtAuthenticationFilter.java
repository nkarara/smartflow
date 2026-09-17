package com.smartflow.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filtre d'authentification JWT, exécuté ONCE pour chaque requête entrante.
 *
 * <p>Fonctionnement :
 * <ol>
 *   <li>lit le header {@code Authorization: Bearer <token>} ;</li>
 *   <li>valide le JWT (signature + expiration) ;</li>
 *   <li>charge l'utilisateur correspondant et place son authentification
 *       dans le {@link SecurityContextHolder} (accessible ensuite par
 *       {@code @PreAuthorize} et {@link SecurityUtils#currentUser()}).</li>
 * </ol>
 * </p>
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    /** Service capable de valider le JWT et d'en extraire l'email. */
    private final JwtService jwtService;

    /** Service de chargement de l'utilisateur à partir de l'email. */
    private final UserDetailsServiceImpl userDetailsService;

    /**
     * Applique l'authentification JWT puis transmet la requête au filtre suivant.
     *
     * @param request     la requête HTTP entrante
     * @param response    la réponse HTTP
     * @param filterChain la chaîne de filtres Spring
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        // 1. Extraction du header Authorization
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);

        // 2. Si le header est présent, de type « Bearer » et aucune session existante
        if (header != null && header.startsWith("Bearer ")
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            String token = header.substring(7); // retire le préfixe « Bearer »
            try {
                // 3. Validation du token et chargement de l'utilisateur
                if (jwtService.isAccessTokenValid(token)) {
                    String email = jwtService.extractEmail(token);
                    UserDetails userDetails = userDetailsService.loadUserByUsername(email);

                    // 4. Construction de l'objet d'authentification avec ses autorités ROLE_xxx
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    // 5. Injection dans le contexte de sécurité (utilisateur « connecté »)
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (Exception ignored) {
                // Token invalide → aucune authentification : la requête restera « non authentifiée »
                SecurityContextHolder.clearContext();
            }
        }
        // 6. Poursuite de la chaîne de filtres (le contrôleur sera ou non protégé selon l'auth)
        filterChain.doFilter(request, response);
    }
}