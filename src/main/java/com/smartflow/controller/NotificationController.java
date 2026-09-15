package com.smartflow.controller;

import com.smartflow.dto.NotificationDtos;
import com.smartflow.security.SecurityUtils;
import com.smartflow.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Notifications de l'utilisateur connecté.
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public List<NotificationDtos.NotificationResponse> list(@RequestParam(defaultValue = "false") boolean unreadOnly) {
        return notificationService.listForUser(SecurityUtils.currentUserId(), unreadOnly);
    }

    @GetMapping("/unread-count")
    public NotificationDtos.UnreadCount unreadCount() {
        return new NotificationDtos.UnreadCount(notificationService.unreadCount(SecurityUtils.currentUserId()));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<Void> markRead(@PathVariable Long id) {
        notificationService.markRead(SecurityUtils.currentUserId(), id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/read-all")
    public ResponseEntity<Void> markAllRead() {
        notificationService.markAllRead(SecurityUtils.currentUserId());
        return ResponseEntity.noContent().build();
    }
}