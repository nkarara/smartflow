package com.smartflow.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Origines CORS autorisées.
 */
@ConfigurationProperties(prefix = "smartflow.cors")
public record CorsProperties(List<String> allowedOrigins) {
}