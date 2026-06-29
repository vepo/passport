package dev.vepo.passport.notification.purge;

import java.time.Duration;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.vepo.passport.notification.NotificationService;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class PurgeOldReadNotificationsTask {

    private static final Logger logger = LoggerFactory.getLogger(PurgeOldReadNotificationsTask.class);

    private final NotificationService notificationService;
    private final Duration readRetention;

    @Inject
    public PurgeOldReadNotificationsTask(NotificationService notificationService,
                                         @ConfigProperty(name = "passport.notifications.read-retention", defaultValue = "PT48H") Duration readRetention) {
        this.notificationService = notificationService;
        this.readRetention = readRetention;
    }

    @Scheduled(every = "${passport.notifications.purge.interval:1h}", delayed = "60s")
    public void purgeOldReadNotifications() {
        var result = notificationService.purgeOldReadNotifications(readRetention);
        if (result.deletedDeliveries() > 0 || result.deletedNotifications() > 0) {
            logger.info("Purged {} read deliveries and {} orphan notifications older than {}",
                        result.deletedDeliveries(),
                        result.deletedNotifications(),
                        readRetention);
        }
    }
}
