package com.smartflow.service;

import com.smartflow.configuration.FileStorageProperties;
import com.smartflow.dto.AttachmentDtos;
import com.smartflow.entity.Attachment;
import com.smartflow.entity.Intervention;
import com.smartflow.entity.NotificationType;
import com.smartflow.entity.Role;
import com.smartflow.entity.User;
import com.smartflow.exception.BusinessException;
import com.smartflow.exception.ResourceNotFoundException;
import com.smartflow.mapper.Mappers;
import com.smartflow.repository.AttachmentRepository;
import com.smartflow.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;

/**
 * Stockage et consultation des pièces jointes d'une intervention.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AttachmentService {

    private final AttachmentRepository attachmentRepository;
    private final InterventionService interventionService;
    private final NotificationService notificationService;
    private final FileStorageProperties fileStorageProperties;

    @Transactional(readOnly = true)
    public List<AttachmentDtos.AttachmentResponse> list(Long interventionId) {
        interventionService.requireViewAccess(interventionId);
        return attachmentRepository.findByInterventionIdOrderByUploadedAtAsc(interventionId).stream()
                .map(Mappers::toAttachmentResponse)
                .toList();
    }

    @Transactional
    public AttachmentDtos.AttachmentResponse upload(Long interventionId, MultipartFile file) {
        Intervention intervention = interventionService.getAccessible(interventionId);
        User actor = SecurityUtils.currentUser();

        if (file.isEmpty()) {
            throw new BusinessException("Le fichier est vide");
        }
        String originalName = sanitizeFileName(file.getOriginalFilename());

        try {
            Path dir = Paths.get(fileStorageProperties.uploadDir())
                    .resolve("intervention-" + interventionId)
                    .toAbsolutePath().normalize();
            Files.createDirectories(dir);
            String storedName = System.currentTimeMillis() + "-" + originalName;
            Path target = dir.resolve(storedName);
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }

            Attachment attachment = Attachment.builder()
                    .intervention(intervention)
                    .uploadedBy(actor)
                    .fileName(originalName)
                    .contentType(file.getContentType())
                    .size(file.getSize())
                    .filePath(target.toString())
                    .build();
            Attachment saved = attachmentRepository.save(attachment);

            notifyParticipants(intervention, actor);
            return Mappers.toAttachmentResponse(saved);
        } catch (IOException ex) {
            log.error("Échec de l'enregistrement de la pièce jointe", ex);
            throw new BusinessException("Impossible d'enregistrer la pièce jointe");
        }
    }

    @Transactional(readOnly = true)
    public AttachmentFile download(Long interventionId, Long attachmentId) {
        interventionService.requireViewAccess(interventionId);
        Attachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Pièce jointe", attachmentId));
        if (!attachment.getIntervention().getId().equals(interventionId)) {
            throw new BusinessException("Pièce jointe invalide pour cette intervention");
        }
        Resource resource = new FileSystemResource(attachment.getFilePath());
        if (!resource.exists()) {
            throw new ResourceNotFoundException("Fichier introuvable sur le serveur");
        }
        return new AttachmentFile(Mappers.toAttachmentResponse(attachment), resource);
    }

    public record AttachmentFile(AttachmentDtos.AttachmentResponse meta, Resource resource) {
    }

    private void notifyParticipants(Intervention intervention, User actor) {
        Long clientUserId = intervention.getClient().getUser().getId();
        if (!clientUserId.equals(actor.getId())) {
            notificationService.notify(clientUserId, NotificationType.NEW_ATTACHMENT,
                    "Nouveau document sur l'intervention \"" + intervention.getTitle() + "\"",
                    intervention.getId());
        }
        if (intervention.getTechnician() != null
                && !intervention.getTechnician().getUser().getId().equals(actor.getId())) {
            notificationService.notify(intervention.getTechnician().getUser().getId(), NotificationType.NEW_ATTACHMENT,
                    "Nouveau document sur l'intervention \"" + intervention.getTitle() + "\"",
                    intervention.getId());
        }
        if (actor.getRole() == Role.CLIENT) {
            notificationService.notifyRole(Role.MANAGER, NotificationType.NEW_ATTACHMENT,
                    "Le client a ajouté un document à l'intervention \"" + intervention.getTitle() + "\"",
                    intervention.getId());
        }
    }

    private String sanitizeFileName(String name) {
        if (name == null || name.isBlank()) {
            return "fichier";
        }
        return Paths.get(name).getFileName().toString().replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}