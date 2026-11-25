package com.ridematch.dto.community;

import java.time.LocalDateTime;

import com.ridematch.domain.entity.Notification;
import com.ridematch.domain.enums.NotificationType;

/** Safe in-app notification view. */
public record NotificationResponse(Long id, NotificationType type, String title, String message,
        Long rideId, LocalDateTime createdAt, LocalDateTime readAt) {

    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(notification.getId(), notification.getType(),
                notification.getTitle(), notification.getMessage(),
                notification.getRide() == null ? null : notification.getRide().getId(),
                notification.getCreatedAt(), notification.getReadAt());
    }
}
