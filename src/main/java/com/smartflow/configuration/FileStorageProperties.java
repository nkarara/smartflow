package com.smartflow.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Répertoire de stockage des pièces jointes.
 */
@ConfigurationProperties(prefix = "smartflow")
public record FileStorageProperties(String uploadDir) {
}