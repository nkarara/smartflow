package com.smartflow.service;

import com.smartflow.dto.CommentDtos;
import com.smartflow.entity.Comment;
import com.smartflow.entity.Intervention;
import com.smartflow.entity.NotificationType;
import com.smartflow.entity.Role;
import com.smartflow.entity.User;
import com.smartflow.mapper.Mappers;
import com.smartflow.repository.CommentRepository;
import com.smartflow.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Gestion des commentaires sur une intervention.
 */
@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final InterventionService interventionService;
    private final NotificationService notificationService;

    @Transactional(readOnly = true)
    public List<CommentDtos.CommentResponse> listForIntervention(Long interventionId) {
        interventionService.requireViewAccess(interventionId);
        return commentRepository.findByInterventionIdOrderByCreatedAtAsc(interventionId).stream()
                .map(Mappers::toCommentResponse)
                .toList();
    }

    @Transactional
    public CommentDtos.CommentResponse add(Long interventionId, CommentDtos.CommentRequest request) {
        Intervention intervention = interventionService.getAccessible(interventionId);
        User author = SecurityUtils.currentUser();

        Comment comment = Comment.builder()
                .intervention(intervention)
                .author(author)
                .content(request.content().trim())
                .build();
        Comment saved = commentRepository.save(comment);

        notifyParticipants(intervention, author);
        return Mappers.toCommentResponse(saved);
    }

    private void notifyParticipants(Intervention intervention, User author) {
        Long clientUserId = intervention.getClient().getUser().getId();
        if (!clientUserId.equals(author.getId())) {
            notificationService.notify(clientUserId, NotificationType.NEW_COMMENT,
                    "Nouveau commentaire sur l'intervention \"" + intervention.getTitle() + "\"",
                    intervention.getId());
        }
        if (intervention.getTechnician() != null
                && !intervention.getTechnician().getUser().getId().equals(author.getId())) {
            notificationService.notify(intervention.getTechnician().getUser().getId(), NotificationType.NEW_COMMENT,
                    "Nouveau commentaire sur l'intervention \"" + intervention.getTitle() + "\"",
                    intervention.getId());
        }
        if (author.getRole() == Role.CLIENT) {
            notificationService.notifyRole(Role.MANAGER, NotificationType.NEW_COMMENT,
                    "Le client a commenté l'intervention \"" + intervention.getTitle() + "\"",
                    intervention.getId());
        }
    }
}