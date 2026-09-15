package com.smartflow.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration du fournisseur IA (LLM).
 */
@ConfigurationProperties(prefix = "smartflow.ai")
public record AiProperties(
        String provider,
        String baseUrl,
        String apiKey,
        String model
) {
    public boolean isEnabled() {
        return apiKey != null && !apiKey.isBlank();
    }
}