package com.smartflow.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propriétés JWT (durées, secret).
 */
@ConfigurationProperties(prefix = "smartflow.jwt")
public record JwtProperties(
        String secret,
        long accessTokenValidityMinutes,
        long refreshTokenValidityDays
) {
}