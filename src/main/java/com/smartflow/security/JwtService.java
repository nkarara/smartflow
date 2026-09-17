package com.smartflow.security;

import com.smartflow.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

/**
 * Service de gestion des jetons JWT.
 *
 * <p>Deux familles de jetons :
 * <ul>
 *   <li><b>Access token</b> : JWT signé HMAC-SHA, court (15 min), envoyé dans le
 *       header {@code Authorization: Bearer ...} à chaque requête.</li>
 *   <li><b>Refresh token</b> : jeton opaque (UUID), stocké en base via
 *       {@link com.smartflow.entity.RefreshToken}, utilisé pour renouveler l'access token.</li>
 * </ul>
 * </p>
 */
@Service
public class JwtService {

    /** Propriétés de configuration (secret + durées). */
    private final JwtProperties properties;

    /** Clé symétrique HMAC dérivée du secret de configuration. */
    private final SecretKey key;

    /**
     * Construit le service et dérive la clé de signature depuis le secret.
     * Un secret trop court (< 32 octets) empêche le démarrage (sécurité).
     *
     * @param properties configuration JWT injectée par Spring
     */
    public JwtService(JwtProperties properties) {
        this.properties = properties;
        // Convertit le secret en octets UTF-8 pour créer une clé HMAC-SHA
        byte[] keyBytes = properties.secret().getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalStateException("JWT_SECRET doit contenir au moins 32 caractères");
        }
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Génère un access token JWT contenant l'utilisateur (email), son identifiant
     * ({@code uid}) et son rôle ({@code role}), avec une date d'expiration.
     *
     * @param user l'utilisateur authentifié
     * @return le JWT signé, prêt à être envoyé au client
     */
    public String generateAccessToken(User user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(user.getEmail())                                   // identifiant principal : email
                .claim("uid", user.getId())                                 // charge utile : identifiant
                .claim("role", user.getRole().name())                       // charge utile : rôle
                .issuedAt(Date.from(now))                                   // date d'émission
                .expiration(Date.from(now.plus(properties.accessTokenValidityMinutes(), ChronoUnit.MINUTES))) // expiration
                .signWith(key)                                              // signature HMAC-SHA
                .compact();
    }

    /**
     * Génère un refresh token opaque (UUID sans tirets) : il n'est pas un JWT —
     * sa valeur est stockée en base et servira de « preuve» lors du rafraîchissement.
     *
     * @return une chaîne aléatoire unique de 32 caractères hexadécimaux
     */
    public String generateRefreshToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * Valide un access token (signature + expiration) sans lever d'exception.
     *
     * @param token le JWT à vérifier
     * @return {@code true} si le jeton est valide
     */
    public boolean isAccessTokenValid(String token) {
        try {
            parseAccessToken(token);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    /**
     * Extrait l'email (subject) contenu dans un access token valide.
     *
     * @param token le JWT
     * @return l'adresse email de l'utilisateur
     */
    public String extractEmail(String token) {
        return parseAccessToken(token).getSubject();
    }

    /**
     * Extrait l'identifiant utilisateur ({@code uid}) contenu dans le JWT.
     *
     * @param token le JWT
     * @return l'identifiant de l'utilisateur
     */
    public long extractUserId(String token) {
        Object uid = parseAccessToken(token).get("uid");
        return uid instanceof Number n ? n.longValue() : Long.parseLong(String.valueOf(uid));
    }

    /**
     * Analyse un access token en vérifiant sa signature HMAC.
     *
     * @param token le JWT à analyser
     * @return les claims (payload) du jeton
     */
    private Claims parseAccessToken(String token) {
        return Jwts.parser()
                .verifyWith(key)        // vérifie la signature avec notre clé
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}