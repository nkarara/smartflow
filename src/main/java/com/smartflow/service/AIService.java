package com.smartflow.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartflow.ai.OpenAiClient;
import com.smartflow.dto.AiDtos;
import com.smartflow.entity.Priority;
import com.smartflow.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

/**
 * Fonctionnalités IA :
 * <ul>
 *   <li>classification automatique d'une demande (catégorie, priorité, temps estimé, problème probable) ;</li>
 *   <li>génération d'un résumé à partir du compte rendu du technicien.</li>
 * </ul>
 * Si une clé API LLM est configurée (smartflow.ai.api-key), un LLM est utilisé ;
 * sinon un analyseur local par mots-clés prend le relais (mode démo hors ligne).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AIService {

    private final OpenAiClient openAiClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final CategoryRepository categoryRepository;

    private record CategoryHint(String category, String type, String probableProblem,
                                int estimatedMinutes, List<String> keywords) {
    }

    private static final List<CategoryHint> HINTS = List.of(
            new CategoryHint("Imprimante", "Matériel",
                    "Erreur matérielle, bourrage papier ou problème de connexion de l'imprimante. Vérifier les consommables et le pilote.",
                    60, List.of("imprimante", "print", "scanner", "scan", "toner", "photocopieur", "fax", "impression")),
            new CategoryHint("Matériel informatique", "Matériel",
                    "Panne matérielle détectée : diagnostic et remplacement d'équipement nécessaires sur le poste.",
                    90, List.of("ordinateur", "ecran", "écran", "clavier", "souris", "disque", "carte mere",
                            "carte mère", "mémoire", "processeur", "ventilateur", "ne demarre",
                            "ne démarre", "allume plus", "s'allume")),
            new CategoryHint("Réseau", "Réseau",
                    "Problème de connectivité réseau : panne de liaison, configuration de l'équipement ou intervention fournisseur requise.",
                    45, List.of("reseau", "réseau", "internet", "wifi", "wi-fi", "connexion", "routeur",
                            "switch", "modem", "vpn", "cable", "câble")),
            new CategoryHint("Logiciel", "Logiciel",
                    "Problème logiciel : réinstallation, mise à jour, dépannage système ou configuration applicative.",
                    40, List.of("logiciel", "application", "email", "messagerie", "word", "excel", "outlook",
                            "windows", "linux", "mise a jour", "mise à jour", "maj", "installation",
                            "virus", "bug", "plante")),
            new CategoryHint("Électricité", "Électricité",
                    "Problème électrique : vérification de l'alimentation, du disjoncteur et du câblage sur site.",
                    120, List.of("electricite", "électricité", "prise", "courant", "disjoncteur",
                            "alimentation", "câblage", "eclairage", "éclairage"))
    );

    private static final List<String> URGENT_MARKERS = List.of(
            "urgent", "urgence", "critique", "bloqu", "ne demarre", "ne démarre",
            "ne s'allume", "panne totale", "hors service");
    private static final List<String> HIGH_MARKERS = List.of(
            "haute", "panne", "important", "rapidement", "sans delai", "sans délai");

    public AiDtos.AnalyzeResponse analyze(String title, String description) {
        if (openAiClient.isEnabled()) {
            try {
                return analyzeWithLlm(title, description);
            } catch (Exception ex) {
                log.warn("Analyse LLM indisponible, repli sur l'analyseur local : {}", ex.getMessage());
            }
        }
        return analyzeLocally(title, description);
    }

    public AiDtos.SummarizeResponse summarize(String reportText, String title, String actions) {
        if (openAiClient.isEnabled()) {
            try {
                String system = "Tu es un assistant qui rédige des résumés concis d'interventions techniques en français.";
                String prompt = (title != null && !title.isBlank() ? "Titre: " + title + "\n" : "")
                        + "Compte rendu: " + reportText
                        + "\nRédige un résumé de 2 à 4 phrases.";
                return new AiDtos.SummarizeResponse(openAiClient.complete(system, prompt).trim(), "LLM");
            } catch (Exception ex) {
                log.warn("Résumé LLM indisponible, repli local : {}", ex.getMessage());
            }
        }
        return new AiDtos.SummarizeResponse(summarizeLocally(reportText, title, actions), "Analyseur local");
    }

    private AiDtos.AnalyzeResponse analyzeWithLlm(String title, String description) {
        String system = "Tu es un assistant de dépannage informatique. Réponds uniquement en JSON valide "
                + "avec les champs : category (string), type (string), priority (LOW|MEDIUM|HIGH|URGENT), "
                + "probableProblem (string, en français), estimatedTimeMinutes (number), reason (string, en français).";
        String prompt = "Titre: " + title + "\nDescription: " + description;
        String raw = openAiClient.complete(system, prompt);
        JsonNode node;
        try {
            node = objectMapper.readTree(raw.trim());
        } catch (com.fasterxml.jackson.core.JsonProcessingException ex) {
            throw new IllegalStateException("Réponse LLM non JSON", ex);
        }
        String categoryName = node.path("category").asText("Général");
        Long categoryId = categoryRepository.findByNameIgnoreCase(categoryName)
                .map(category -> category.getId()).orElse(null);
        return new AiDtos.AnalyzeResponse(
                categoryName,
                categoryId,
                node.path("type").asText("Général"),
                parsePriority(node.path("priority").asText()),
                node.path("probableProblem").asText(),
                node.path("estimatedTimeMinutes").asInt(60),
                "LLM");
    }

    private AiDtos.AnalyzeResponse analyzeLocally(String title, String description) {
        String text = ((title == null ? "" : title) + " " + description).toLowerCase(Locale.ROOT);

        CategoryHint hint = HINTS.stream()
                .filter(h -> h.keywords().stream().anyMatch(text::contains))
                .findFirst()
                .orElse(new CategoryHint("Général", "Général",
                        "Analyse sur site nécessaire pour identifier la cause exacte du problème.", 60, List.of()));

        Priority priority = detectPriority(text);
        Long categoryId = categoryRepository.findByNameIgnoreCase(hint.category())
                .map(category -> category.getId()).orElse(null);

        return new AiDtos.AnalyzeResponse(
                hint.category(), categoryId, hint.type(), priority,
                hint.probableProblem(), hint.estimatedMinutes(), "Analyseur local");
    }

    private Priority detectPriority(String text) {
        if (URGENT_MARKERS.stream().anyMatch(text::contains)) {
            return Priority.URGENT;
        }
        if (HIGH_MARKERS.stream().anyMatch(text::contains)) {
            return Priority.HIGH;
        }
        return Priority.MEDIUM;
    }

    private Priority parsePriority(String value) {
        return switch (value.trim().toUpperCase(Locale.ROOT)) {
            case "URGENT" -> Priority.URGENT;
            case "HIGH" -> Priority.HIGH;
            case "LOW" -> Priority.LOW;
            default -> Priority.MEDIUM;
        };
    }

    public String summarizeLocally(String reportText, String title, String actions) {
        String text = reportText.trim();
        if (text.isEmpty()) {
            return "Aucun compte rendu disponible.";
        }
        List<String> sentences = java.util.Arrays.stream(text.split("[.!?\\n]+"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
        if (sentences.isEmpty()) {
            return "Aucun compte rendu exploitable.";
        }

        List<String> keyWords = List.of("remplac", "install", "configur", "répar", "repar", "nettoy",
                "mise à jour", "mis à jour", "test", "valid");
        List<String> keyActions = sentences.stream()
                .filter(s -> keyWords.stream().anyMatch(s.toLowerCase(Locale.ROOT)::contains))
                .toList();
        List<String> selected = keyActions.isEmpty()
                ? sentences.subList(0, Math.min(3, sentences.size()))
                : keyActions.subList(0, Math.min(2, keyActions.size()));

        StringBuilder summary = new StringBuilder();
        if (title != null && !title.isBlank()) {
            summary.append("Résumé de l'intervention « ").append(title).append(" » : ");
        } else {
            summary.append("Résumé de l'intervention : ");
        }
        summary.append(String.join(". ", selected)).append(".");
        if (actions != null && !actions.isBlank()) {
            summary.append(" Actions réalisées : ").append(actions.trim()).append(".");
        }
        return summary.toString();
    }
}