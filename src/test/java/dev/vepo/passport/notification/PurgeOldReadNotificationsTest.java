package dev.vepo.passport.notification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.vepo.passport.model.Notification;
import dev.vepo.passport.model.UserNotification;
import dev.vepo.passport.shared.Given;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
@DisplayName("Purge old read notifications")
class PurgeOldReadNotificationsTest {

    private static final Duration RETENTION = Duration.ofDays(2);

    @Inject
    NotificationService notificationService;

    @Inject
    NotificationRepository notificationRepository;

    @Inject
    UserNotificationRepository userNotificationRepository;

    @BeforeEach
    void cleanup() {
        Given.cleanup();
    }

    @Test
    @DisplayName("Should delete read deliveries older than retention and orphan notifications")
    void purgeOldReadNotifications_DeletesStaleReadDeliveriesAndOrphanNotifications() {
        var user = Given.user()
                        .withUsername("purge-user")
                        .withEmail("purge@passport.vepo.dev")
                        .withName("Purge User")
                        .withPassword("password123")
                        .persist()
                        .user();

        var notification = notificationRepository.save(new Notification("engage",
                                                                        "video_sync",
                                                                        1L,
                                                                        "Relatório antigo",
                                                                        "Sync",
                                                                        "{}"));
        var delivery = new UserNotification(user, notification);
        delivery.markRead();
        delivery.setReadAt(Instant.now().minus(3, ChronoUnit.DAYS));
        userNotificationRepository.save(delivery);

        var result = notificationService.purgeOldReadNotifications(RETENTION);

        assertEquals(1, result.deletedDeliveries());
        assertEquals(1, result.deletedNotifications());
        assertTrue(notificationRepository.findById(notification.getId()).isEmpty());
        assertTrue(userNotificationRepository.findByUser(user, null).isEmpty());
    }

    @Test
    @DisplayName("Should keep unread deliveries and recently read deliveries")
    void purgeOldReadNotifications_KeepsUnreadAndRecentReadDeliveries() {
        var user = Given.user()
                        .withUsername("keep-user")
                        .withEmail("keep@passport.vepo.dev")
                        .withName("Keep User")
                        .withPassword("password123")
                        .persist()
                        .user();

        var unreadNotification = notificationRepository.save(new Notification("engage",
                                                                              "video_sync",
                                                                              2L,
                                                                              "Não lida",
                                                                              "Sync",
                                                                              "{}"));
        userNotificationRepository.save(new UserNotification(user, unreadNotification));

        var recentReadNotification = notificationRepository.save(new Notification("engage",
                                                                                  "comment_sync",
                                                                                  2L,
                                                                                  "Lida recente",
                                                                                  "Sync",
                                                                                  "{}"));
        var recentDelivery = new UserNotification(user, recentReadNotification);
        recentDelivery.markRead();
        recentDelivery.setReadAt(Instant.now().minus(1, ChronoUnit.DAYS));
        userNotificationRepository.save(recentDelivery);

        var result = notificationService.purgeOldReadNotifications(RETENTION);

        assertEquals(0, result.deletedDeliveries());
        assertEquals(0, result.deletedNotifications());
        assertEquals(2, userNotificationRepository.findByUser(user, null).size());
    }

    @Test
    @DisplayName("Should keep notification when another user still has a delivery")
    void purgeOldReadNotifications_KeepsNotificationWhileOtherDeliveriesRemain() {
        var firstUser = Given.user()
                             .withUsername("first-user")
                             .withEmail("first@passport.vepo.dev")
                             .withName("First User")
                             .withPassword("password123")
                             .persist()
                             .user();
        var secondUser = Given.user()
                              .withUsername("second-user")
                              .withEmail("second@passport.vepo.dev")
                              .withName("Second User")
                              .withPassword("password123")
                              .persist()
                              .user();

        var notification = notificationRepository.save(new Notification("engage",
                                                                        "video_sync",
                                                                        3L,
                                                                        "Compartilhada",
                                                                        "Sync",
                                                                        "{}"));

        var oldReadDelivery = new UserNotification(firstUser, notification);
        oldReadDelivery.markRead();
        oldReadDelivery.setReadAt(Instant.now().minus(3, ChronoUnit.DAYS));
        userNotificationRepository.save(oldReadDelivery);

        userNotificationRepository.save(new UserNotification(secondUser, notification));

        var result = notificationService.purgeOldReadNotifications(RETENTION);

        assertEquals(1, result.deletedDeliveries());
        assertEquals(0, result.deletedNotifications());
        assertTrue(notificationRepository.findById(notification.getId()).isPresent());
        assertEquals(1, userNotificationRepository.findByUser(secondUser, null).size());
        assertTrue(userNotificationRepository.findByUser(firstUser, null).isEmpty());
    }
}
