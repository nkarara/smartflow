package com.smartflow.ai;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * Client HTTP vers un LLM compatible API OpenAI (chat/completions).
 * Active uniquement lorsque smartflow.ai.api-key est renseigné.
 */
@Component
public class OpenAiClient {

    private final RestClient restClient;
    private final AiProperties properties;

    public OpenAiClient(AiProperties properties) {
        this.properties = properties;
        this.restClient = RestClient.builder()
                .baseUrl(properties.baseUrl())
                .defaultHeader("Authorization", "Bearer " + properties.apiKey())
                .build();
    }

    public boolean isEnabled() {
        return properties.isEnabled();
    }

    public String complete(String systemPrompt, String userPrompt) {
        Map<String, Object> body = Map.of(
                "model", properties.model(),
                "temperature", 0.2,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)
                )
        );
        JsonNode response = restClient.post()
                .uri("/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(JsonNode.class);
        if (response == null) {
            throw new IllegalStateException("Réponse LLM vide");
        }
        return response.path("choices").get(0).path("message").path("content").asText();
    }
}