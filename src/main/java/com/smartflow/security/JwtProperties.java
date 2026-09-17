package com.smartflow.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propriétés de configuration du JWT, liées à la section {@code smartflow.jwt}
 * du fichier {@code application.yml} (secret + durées de validité).
 *
 * <p>Le binding est réalisé automatiquement par Spring grâce à
 * {@code @ConfigurationPropertiesScan} sur la classe principale.</p>
 *
 * @param secret                      clé HMAC (≥ 32 caractères) signant les tokens
 * @param accessTokenValidityMinutes  durée de vie de l'access token (défaut 15 min)
 * @param refreshTokenValidityDays    durée de vie du refresh token (défaut 7 jours)
 */
@ConfigurationProperties(prefix = "smartflow.jwt")
public record JwtProperties(
        String secret,
        long accessTokenValidityMinutes,
        long refreshTokenValidityDays
) {
}