package com.smartflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * SmartFlow — Application de gestion des interventions techniques.
 *
 * <p>Point d'entrée Spring Boot :
 * <ul>
 *   <li>{@code @SpringBootApplication} = configuration automatique + scan des composants
 *       ({@code @Service}, {@code @Controller}, {@code @Repository}… du package
 *       {@code com.smartflow} et de ses sous-paquets) + abréviation de
 *       {@code @Configuration} + {@code @EnableAutoConfiguration} + {@code @ComponentScan} ;</li>
 *   <li>{@code @ConfigurationPropertiesScan} : enregistre automatiquement toutes les classes
 *       annotées {@code @ConfigurationProperties} (JWT, CORS, IA, stockage fichiers).</li>
 * </ul>
 * </p>
 *
 * <p>Démarrage : {@code mvn spring-boot:run} ou {@code mvnw spring-boot:run}.</p>
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class SmartFlowApplication {

    /**
     * Point d'entrée : lance le conteneur Spring (Tomcat intégré, port {@code server.port}).
     *
     * @param args arguments de ligne de commande (options Spring via {@code --propriete=valeur})
     */
    public static void main(String[] args) {
        SpringApplication.run(SmartFlowApplication.class, args);
    }
}