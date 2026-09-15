package com.smartflow.entity;

/**
 * Types de notifications générées par le système.
 */
public enum NotificationType {
    NEW_INTERVENTION,
    INTERVENTION_ASSIGNED,
    INTERVENTION_ACCEPTED,
    STATUS_CHANGED,
    NEW_COMMENT,
    NEW_ATTACHMENT,
    NEW_RATING,
    INTERVENTION_RESOLVED,
    INTERVENTION_CLOSED
}