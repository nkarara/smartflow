package com.smartflow.mapper;

import com.smartflow.dto.AttachmentDtos;
import com.smartflow.dto.CommentDtos;
import com.smartflow.dto.NotificationDtos;
import com.smartflow.dto.RatingDtos;
import com.smartflow.entity.Attachment;
import com.smartflow.entity.Comment;
import com.smartflow.entity.Notification;
import com.smartflow.entity.Rating;

/**
 * Mappers des entités secondaires vers leurs DTOs.
 */
public final class Mappers {

    private Mappers() {
    }

    public static CommentDtos.CommentResponse toCommentResponse(Comment comment) {
        return new CommentDtos.CommentResponse(
                comment.getId(),
                comment.getAuthor().getId(),
                comment.getAuthor().getFirstName() + " " + comment.getAuthor().getLastName(),
                comment.getContent(),
                comment.getCreatedAt()
        );
    }

    public static AttachmentDtos.AttachmentResponse toAttachmentResponse(Attachment attachment) {
        return new AttachmentDtos.AttachmentResponse(
                attachment.getId(),
                attachment.getIntervention().getId(),
                attachment.getFileName(),
                attachment.getContentType(),
                attachment.getSize(),
                attachment.getUploadedBy().getId(),
                attachment.getUploadedBy().getFirstName() + " " + attachment.getUploadedBy().getLastName(),
                attachment.getUploadedAt(),
                "/api/interventions/" + attachment.getIntervention().getId()
                        + "/attachments/" + attachment.getId() + "/download"
        );
    }

    public static RatingDtos.RatingResponse toRatingResponse(Rating rating) {
        return new RatingDtos.RatingResponse(
                rating.getId(),
                rating.getIntervention().getId(),
                rating.getScore(),
                rating.getComment(),
                rating.getCreatedAt()
        );
    }

    public static NotificationDtos.NotificationResponse toNotificationResponse(Notification notification) {
        return new NotificationDtos.NotificationResponse(
                notification.getId(),
                notification.getUser().getId(),
                notification.getType(),
                notification.getMessage(),
                notification.getRelatedInterventionId(),
                notification.isRead(),
                notification.getCreatedAt()
        );
    }
}