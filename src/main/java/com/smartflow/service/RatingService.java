package com.smartflow.service;

import com.smartflow.dto.RatingDtos;
import com.smartflow.entity.Intervention;
import com.smartflow.entity.Rating;
import com.smartflow.entity.Role;
import com.smartflow.entity.Status;
import com.smartflow.entity.User;
import com.smartflow.exception.BusinessException;
import com.smartflow.exception.ResourceNotFoundException;
import com.smartflow.mapper.Mappers;
import com.smartflow.repository.RatingRepository;
import com.smartflow.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Évaluation d'une intervention par le client.
 */
@Service
@RequiredArgsConstructor
public class RatingService {

    private final RatingRepository ratingRepository;
    private final InterventionService interventionService;
    private final NotificationService notificationService;

    @Transactional
    public RatingDtos.RatingResponse rate(Long interventionId, RatingDtos.RatingRequest request) {
        Intervention intervention = interventionService.getAccessible(interventionId);
        User actor = SecurityUtils.currentUser();

        if (actor.getRole() != Role.CLIENT
                || !intervention.getClient().getUser().getId().equals(actor.getId())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "Seul le client peut évaluer cette intervention");
        }
        if (intervention.getStatus() != Status.RESOLVED && intervention.getStatus() != Status.CLOSED) {
            throw new BusinessException("L'intervention doit être résolue ou clôturée pour être évaluée");
        }

        Rating rating = ratingRepository.findByInterventionId(interventionId).orElse(null);
        if (rating == null) {
            rating = Rating.builder()
                    .intervention(intervention)
                    .score(request.score())
                    .comment(request.comment())
                    .build();
        } else {
            rating.setScore(request.score());
            rating.setComment(request.comment());
        }
        Rating saved = ratingRepository.save(rating);

        if (intervention.getTechnician() != null) {
            notificationService.notify(intervention.getTechnician().getUser().getId(),
                    com.smartflow.entity.NotificationType.NEW_RATING,
                    "Le client a évalué l'intervention \"" + intervention.getTitle() + "\" (note: "
                            + request.score() + "/5)", intervention.getId());
        }
        return Mappers.toRatingResponse(saved);
    }

    @Transactional(readOnly = true)
    public RatingDtos.RatingResponse get(Long interventionId) {
        interventionService.requireViewAccess(interventionId);
        Rating rating = ratingRepository.findByInterventionId(interventionId)
                .orElseThrow(() -> new ResourceNotFoundException("Évaluation (intervention)", interventionId));
        return Mappers.toRatingResponse(rating);
    }
}