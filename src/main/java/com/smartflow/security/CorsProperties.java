package com.smartflow.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Origines CORS autorisées à appeler l'API, liées à {@code smartflow.cors.allowed-origins}.
 *
 * <p>En développement, le frontend Vite tourne sur {@code http://localhost:5173} ;
 * en Docker, l'application est servie par nginx sur {@code http://localhost:8081}.</p>
 *
 * @param allowedOrigins liste des origines (séparées par des virgules dans la config)
 */
@ConfigurationProperties(prefix = "smartflow.cors")
public record CorsProperties(List<String> allowedOrigins) {
}