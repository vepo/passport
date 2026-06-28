package dev.vepo.passport.notification;

import java.time.Instant;

import dev.vepo.passport.model.NotificationItem;

public record NotificationItemResponse(Long id,
                                       String title,
                                       String description,
                                       String report,
                                       int sequence) {
    public static NotificationItemResponse from(NotificationItem item) {
        return new NotificationItemResponse(item.getId(),
                                            item.getTitle(),
                                            item.getDescription(),
                                            item.getReport(),
                                            item.getSequence());
    }
}
