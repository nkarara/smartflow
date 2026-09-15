package com.smartflow.service;

import com.smartflow.dto.NotificationDtos;
import com.smartflow.entity.Notification;
import com.smartflow.entity.NotificationType;
import com.smartflow.entity.Role;
import com.smartflow.entity.User;
import com.smartflow.exception.BusinessException;
import com.smartflow.exception.ResourceNotFoundException;
import com.smartflow.mapper.Mappers;
import com.smartflow.repository.NotificationRepository;
import com.smartflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Création et consultation des notifications.
 */
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @Transactional
    public void notify(Long userId, NotificationType type, String message, Long interventionId) {
        userRepository.findById(userId).ifPresent(user ->
                notificationRepository.save(Notification.builder()
                        .user(user)
                        .type(type)
                        .message(message)
                        .relatedInterventionId(interventionId)
                        .build()));
    }

    @Transactional
    public void notifyRole(Role role, NotificationType type, String message, Long interventionId) {
        userRepository.findAll().stream()
                .filter(user -> user.getRole() == role && user.isEnabled())
                .forEach(user -> notify(user.getId(), type, message, interventionId));
    }

    @Transactional(readOnly = true)
    public List<NotificationDtos.NotificationResponse> listForUser(Long userId, boolean unreadOnly) {
        List<Notification> items = unreadOnly
                ? notificationRepository.findByUserIdAndReadFalseOrderByCreatedAtDesc(userId)
                : notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return items.stream().map(Mappers::toNotificationResponse).toList();
    }

    public long unreadCount(Long userId) {
        return notificationRepository.countByUserIdAndReadFalse(userId);
    }

    @Transactional
    public void markRead(Long userId, Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", notificationId));
        if (!notification.getUser().getId().equals(userId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "Cette notification ne vous appartient pas");
        }
        notification.setRead(true);
        notificationRepository.save(notification);
    }

    @Transactional
    public long markAllRead(Long userId) {
        List<Notification> unread = notificationRepository.findByUserIdAndReadFalseOrderByCreatedAtDesc(userId);
        unread.forEach(notification -> notification.setRead(true));
        notificationRepository.saveAll(unread);
        return unread.size();
    }
}