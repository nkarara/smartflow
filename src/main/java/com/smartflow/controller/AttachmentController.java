package com.smartflow.controller;

import com.smartflow.dto.AttachmentDtos;
import com.smartflow.service.AttachmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Pièces jointes d'une intervention (photos, documents).
 */
@RestController
@RequestMapping("/api/interventions/{interventionId}/attachments")
@RequiredArgsConstructor
public class AttachmentController {

    private final AttachmentService attachmentService;

    @GetMapping
    public List<AttachmentDtos.AttachmentResponse> list(@PathVariable Long interventionId) {
        return attachmentService.list(interventionId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AttachmentDtos.AttachmentResponse upload(@PathVariable Long interventionId,
                                                    @RequestParam("file") MultipartFile file) {
        return attachmentService.upload(interventionId, file);
    }

    @GetMapping("/{attachmentId}/download")
    public ResponseEntity<Resource> download(@PathVariable Long interventionId,
                                             @PathVariable Long attachmentId) {
        AttachmentService.AttachmentFile file = attachmentService.download(interventionId, attachmentId);
        MediaType mediaType;
        try {
            mediaType = MediaType.parseMediaType(file.meta().contentType());
        } catch (Exception ex) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + file.meta().fileName() + "\"")
                .body(file.resource());
    }
}