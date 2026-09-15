package com.smartflow.controller;

import com.smartflow.dto.CommentDtos;
import com.smartflow.service.CommentService;
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

import java.util.List;

/**
 * Commentaires sur une intervention.
 */
@RestController
@RequestMapping("/api/interventions/{interventionId}/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @GetMapping
    public List<CommentDtos.CommentResponse> list(@PathVariable Long interventionId) {
        return commentService.listForIntervention(interventionId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CommentDtos.CommentResponse add(@PathVariable Long interventionId,
                                           @Valid @RequestBody CommentDtos.CommentRequest request) {
        return commentService.add(interventionId, request);
    }
}