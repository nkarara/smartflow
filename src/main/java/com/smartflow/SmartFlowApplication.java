package com.smartflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * SmartFlow - Application de gestion des interventions techniques.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class SmartFlowApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartFlowApplication.class, args);
    }
}