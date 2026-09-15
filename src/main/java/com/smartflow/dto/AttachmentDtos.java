package com.smartflow.dto;

import java.time.LocalDateTime;

/**
 * DTOs de gestion des pièces jointes.
 */
public final class AttachmentDtos {

    private AttachmentDtos() {
    }

    public record AttachmentResponse(
            Long id,
            Long interventionId,
            String fileName,
            String contentType,
            long size,
            Long uploadedById,
            String uploadedByName,
            LocalDateTime uploadedAt,
            String downloadUrl
    ) {
    }
}