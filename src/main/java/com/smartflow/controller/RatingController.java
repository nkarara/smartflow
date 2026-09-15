package com.smartflow.controller;

import com.smartflow.dto.RatingDtos;
import com.smartflow.service.RatingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Évaluation d'une intervention par le client.
 */
@RestController
@RequestMapping("/api/interventions/{interventionId}/rating")
@RequiredArgsConstructor
public class RatingController {

    private final RatingService ratingService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RatingDtos.RatingResponse rate(@PathVariable Long interventionId,
                                          @Valid @RequestBody RatingDtos.RatingRequest request) {
        return ratingService.rate(interventionId, request);
    }

    @GetMapping
    public RatingDtos.RatingResponse get(@PathVariable Long interventionId) {
        return ratingService.get(interventionId);
    }
}