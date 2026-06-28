package dev.vepo.passport.notification;

import java.time.Instant;
import java.util.List;

import dev.vepo.passport.model.Notification;
import dev.vepo.passport.model.NotificationItem;

public record NotificationResponse(Long id,
                                   String sourceService,
                                   String sourceType,
                                   Long engageChannelId,
                                   String title,
                                   String description,
                                   String report,
                                   boolean read,
                                   Instant createdAt,
                                   List<NotificationItemResponse> items) {
    public static NotificationResponse fromDelivery(Long userNotificationId, Notification notification, boolean read) {
        var itemResponses = notification.getItems()
                                        .stream()
                                        .map(NotificationItemResponse::from)
                                        .toList();
        return new NotificationResponse(notification.getId(),
                                        notification.getSourceService(),
                                        notification.getSourceType(),
                                        notification.getEngageChannelId(),
                                        notification.getTitle(),
                                        notification.getDescription(),
                                        notification.getReport(),
                                        read,
                                        notification.getCreatedAt(),
                                        itemResponses);
    }
}
