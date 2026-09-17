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
 *
 * <p>Les notifications sont créées par les autres services à chaque événement métier
 * (nouvelle intervention, affectation, commentaire…) via {@link #notify} pour un
 * utilisateur précis ou {@link #notifyRole} pour tous les utilisateurs d'un rôle.</p>
 */
@Service
@RequiredArgsConstructor
public class NotificationService {

    /** Dépôt d'accès aux notifications. */
    private final NotificationRepository notificationRepository;
    /** Dépôt d'accès aux utilisateurs (résolution des destinataires par rôle). */
    private final UserRepository userRepository;

    /**
     * Crée une notification pour un utilisateur donné (ex. le technicien affecté).
     *
     * @param userId            identifiant du destinataire
     * @param type              type d'événement
     * @param message           message lisible en français
     * @param interventionId    identifiant de l'intervention liée (nullable)
     */
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

    /**
     * Notifie tous les utilisateurs actifs d'un rôle donné
     * (ex. tous les managers lors d'une nouvelle demande).
     *
     * @param role            rôle ciblé
     * @param type            type d'événement
     * @param message         message lisible en français
     * @param interventionId  identifiant de l'intervention liée (nullable)
     */
    @Transactional
    public void notifyRole(Role role, NotificationType type, String message, Long interventionId) {
        // Parcours de tous les utilisateurs du rôle souhaité
        userRepository.findAll().stream()
                .filter(user -> user.getRole() == role && user.isEnabled())
                .forEach(user -> notify(user.getId(), type, message, interventionId));
    }

    /**
     * Liste les notifications d'un utilisateur (les plus récentes d'abord).
     *
     * @param userId     identifiant de l'utilisateur
     * @param unreadOnly si {@code true}, ne conserve que les notifications non lues
     * @return les notifications sous forme de DTO
     */
    @Transactional(readOnly = true)
    public List<NotificationDtos.NotificationResponse> listForUser(Long userId, boolean unreadOnly) {
        List<Notification> items = unreadOnly
                ? notificationRepository.findByUserIdAndReadFalseOrderByCreatedAtDesc(userId)
                : notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return items.stream().map(Mappers::toNotificationResponse).toList();
    }

    /** Nombre de notifications non lues (badge de la cloche dans l'interface). */
    public long unreadCount(Long userId) {
        return notificationRepository.countByUserIdAndReadFalse(userId);
    }

    /**
     * Marque une notification comme lue (vérifie qu'elle appartient bien à l'utilisateur).
     *
     * @param userId         identifiant du propriétaire
     * @param notificationId identifiant de la notification
     */
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

    /**
     * Marque toutes les notifications d'un utilisateur comme lues.
     *
     * @param userId identifiant de l'utilisateur
     * @return le nombre de notifications marquées
     */
    @Transactional
    public long markAllRead(Long userId) {
        List<Notification> unread = notificationRepository.findByUserIdAndReadFalseOrderByCreatedAtDesc(userId);
        unread.forEach(notification -> notification.setRead(true));
        notificationRepository.saveAll(unread);
        return unread.size();
    }
}