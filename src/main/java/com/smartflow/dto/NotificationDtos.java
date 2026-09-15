package com.smartflow.dto;

import com.smartflow.entity.NotificationType;

import java.time.LocalDateTime;

/**
 * DTOs de gestion des notifications.
 */
public final class NotificationDtos {

    private NotificationDtos() {
    }

    public record NotificationResponse(
            Long id,
            Long userId,
            NotificationType type,
            String message,
            Long relatedInterventionId,
            boolean read,
            LocalDateTime createdAt
    ) {
    }

    public record UnreadCount(long count) {
    }
}