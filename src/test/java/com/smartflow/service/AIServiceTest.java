package com.smartflow.service;

import com.smartflow.ai.OpenAiClient;
import com.smartflow.entity.Category;
import com.smartflow.entity.Priority;
import com.smartflow.repository.CategoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * Analyse locale (hors ligne) des demandes et génération de résumés.
 */
@ExtendWith(MockitoExtension.class)
class AIServiceTest {

    @Mock
    private OpenAiClient openAiClient;
    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private AIService aiService;

    @Test
    void localAnalysisClassifiesDescriptionAndPriority() {
        when(openAiClient.isEnabled()).thenReturn(false);
        Category category = new Category();
        category.setId(7L);
        category.setName("Imprimante");
        when(categoryRepository.findByNameIgnoreCase("Imprimante")).thenReturn(Optional.of(category));

        var response = aiService.analyze("Imprimante en panne",
                "L'imprimante du bureau affiche une erreur et ne veut plus imprimer. C'est urgent.");

        assertEquals("Imprimante", response.category());
        assertEquals(7L, response.categoryId());
        assertEquals(Priority.URGENT, response.priority());
        assertEquals(60, response.estimatedTimeMinutes());
        assertTrue(response.probableProblem().contains("l'imprimante"));
    }

    @Test
    void localSummaryExtractsKeyActions() {
        String summary = aiService.summarizeLocally(
                "Remplacement de la carte réseau effectué. Test de connexion concluant. Le poste fonctionne de nouveau.",
                "Coupure réseau", "Remplacement carte réseau");

        assertTrue(summary.contains("Coupure réseau"));
        assertTrue(summary.contains("Remplacement de la carte réseau"));
        assertTrue(summary.contains("Actions réalisées"));
    }
}