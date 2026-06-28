package dev.vepo.passport.notification;

import java.time.Instant;

import dev.vepo.passport.model.Notification;

public record NotificationSummaryResponse(Long id,
                                          String sourceService,
                                          String sourceType,
                                          Long engageChannelId,
                                          String title,
                                          String description,
                                          boolean read,
                                          int itemCount,
                                          Instant createdAt) {
    public static NotificationSummaryResponse from(Notification notification, boolean read) {
        return new NotificationSummaryResponse(notification.getId(),
                                               notification.getSourceService(),
                                               notification.getSourceType(),
                                               notification.getEngageChannelId(),
                                               notification.getTitle(),
                                               notification.getDescription(),
                                               read,
                                               notification.getItems() == null ? 0 : notification.getItems().size(),
                                               notification.getCreatedAt());
    }
}
