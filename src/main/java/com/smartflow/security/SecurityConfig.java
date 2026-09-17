package com.smartflow.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Configuration centrale de Spring Security.
 *
 * <p>Résumé :
 * <ul>
 *   <li>authentification <b>JWT sans état</b> (aucune session HTTP) ;</li>
 *   <li>CSRF désactivé (API REST consommée par un frontend séparé, protégée par JWT) ;</li>
 *   <li>CORS configuré pour autoriser les origines du frontend ;</li>
 *   <li>règles d'accès : {@code /api/auth/**} et {@code /actuator/health} publics,
 *       tout le reste authentifié ;</li>
 *   <li>filtre {@link JwtAuthenticationFilter} inséré avant le filtre de login ;</li>
 *   <li>réponses JSON structurées pour les erreurs 401 et 403 ;</li>
 *   <li>{@code @EnableMethodSecurity} active {@code @PreAuthorize} sur les contrôleurs.</li>
 * </ul>
 * </p>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    /** Filtre JWT maison injecté dans la chaîne. */
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    /** Service de chargement des utilisateurs pour l'authentification. */
    private final UserDetailsServiceImpl userDetailsService;

    /** Origines CORS autorisées (config {@code smartflow.cors}). */
    private final CorsProperties corsProperties;

    /**
     * Encodeur de mots de passe : BCrypt (hachage robuste et auto-salé).
     *
     * @return l'implémentation {@link PasswordEncoder} injectée partout
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Provider d'authentification DAO : compare les identifiants saisis
     * avec la base via {@link UserDetailsServiceImpl} et {@link #passwordEncoder()}.
     *
     * @return le provider utilisé par {@code AuthenticationManager}
     */
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        // Spring Security 7 : le UserDetailsService est passé au constructeur
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    /**
     * Manager d'authentification central, utilisé par {@code AuthService.login(...)}
     * pour valider les couples email/mot de passe.
     *
     * @param configuration configuration d'authentification Spring
     * @return le {@link AuthenticationManager} configuré
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    /**
     * Source CORS : autorise les origines configurées (frontend Vite/nginx),
     * toutes les méthodes HTTP standard et le header {@code Authorization}.
     *
     * @return la configuration CORS appliquée à toutes les routes
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(corsProperties.allowedOrigins()); // origines autorisées
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("Authorization")); // expose le header aux navigateurs
        config.setAllowCredentials(true);                    // cookies/Authorization cross-origin
        config.setMaxAge(3600L);                              // cache de pré-vol (1 h)

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    /**
     * Chaîne de filtres de sécurité principale.
     *
     * @param http le constructeur {@link HttpSecurity}
     * @return la {@link SecurityFilterChain} à appliquer à toutes les requêtes
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)              // pas de session → pas de CSRF
                .cors(cors -> cors.configurationSource(corsConfigurationSource())) // CORS actif
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // JWT : sans état
                .authenticationProvider(authenticationProvider())   // provider BCrypt/DAO
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()        // login/register/refresh : public
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll() // santé : public
                        .anyRequest().authenticated())                     // tout le reste : connecté exigé
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class) // JWT validé en premier
                .exceptionHandling(handling -> handling
                        // 401 : réponse JSON « Authentification requise »
                        .authenticationEntryPoint((request, response, ex) -> {
                            response.setStatus(HttpStatus.UNAUTHORIZED.value());
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            response.setCharacterEncoding("UTF-8");
                            response.getWriter().write("{\"message\":\"Authentification requise\"}");
                        })
                        // 403 : réponse JSON « Accès refusé »
                        .accessDeniedHandler((request, response, ex) -> {
                            response.setStatus(HttpStatus.FORBIDDEN.value());
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            response.setCharacterEncoding("UTF-8");
                            response.getWriter().write("{\"message\":\"Accès refusé\"}");
                        }));
        return http.build();
    }
}