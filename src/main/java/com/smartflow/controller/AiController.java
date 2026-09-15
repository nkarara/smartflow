package com.smartflow.controller;

import com.smartflow.dto.AiDtos;
import com.smartflow.service.AIService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Fonctionnalités d'intelligence artificielle : classification et résumés.
 */
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final AIService aiService;

    @PostMapping("/analyze")
    public AiDtos.AnalyzeResponse analyze(@Valid @RequestBody AiDtos.AnalyzeRequest request) {
        return aiService.analyze(request.title(), request.description());
    }

    @PostMapping("/summarize")
    public AiDtos.SummarizeResponse summarize(@Valid @RequestBody AiDtos.SummarizeRequest request) {
        return aiService.summarize(request.reportText(), request.title(), request.actions());
    }
}