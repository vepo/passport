package dev.vepo.passport.notification;

import java.util.List;

import dev.vepo.passport.model.Notification;
import dev.vepo.passport.model.NotificationItem;
import dev.vepo.passport.model.User;
import dev.vepo.passport.model.UserNotification;
import dev.vepo.passport.channelfollow.ChannelFollowRepository;
import dev.vepo.passport.user.UserRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;

@ApplicationScoped
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserNotificationRepository userNotificationRepository;
    private final ChannelFollowRepository channelFollowRepository;
    private final UserRepository userRepository;

    @Inject
    public NotificationService(NotificationRepository notificationRepository,
                               UserNotificationRepository userNotificationRepository,
                               ChannelFollowRepository channelFollowRepository,
                               UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userNotificationRepository = userNotificationRepository;
        this.channelFollowRepository = channelFollowRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public NotificationResponse publishInternalNotification(CreateInternalNotificationRequest request) {
        var notification = new Notification(request.sourceService(),
                                            request.sourceType(),
                                            request.engageChannelId(),
                                            request.title(),
                                            request.description(),
                                            request.report());

        var sequence = 0;
        if (request.items() != null) {
            for (var item : request.items()) {
                notification.addItem(new NotificationItem(item.title(),
                                                          item.description(),
                                                          item.report(),
                                                          sequence++));
            }
        }

        notificationRepository.save(notification);

        if (request.engageChannelId() != null) {
            channelFollowRepository.findByEngageChannelId(request.engageChannelId())
                                   .forEach(follow -> userNotificationRepository.save(new UserNotification(follow.getUser(),
                                                                                                           notification)));
        }

        return NotificationResponse.fromDelivery(null, notification, false);
    }

    public List<NotificationSummaryResponse> listForUser(String username, Boolean unreadOnly) {
        var user = requireActiveUser(username);
        return userNotificationRepository.findByUser(user, unreadOnly)
                                         .stream()
                                         .map(un -> NotificationSummaryResponse.from(un.getNotification(), un.isRead()))
                                         .toList();
    }

    public long countUnreadForUser(String username) {
        var user = requireActiveUser(username);
        return userNotificationRepository.countUnreadByUser(user);
    }

    @Transactional
    public NotificationResponse findForUser(String username, Long notificationId) {
        var user = requireActiveUser(username);
        var delivery = userNotificationRepository.findByUserAndNotificationId(user, notificationId)
                                                 .orElseThrow(() -> new NotFoundException("Notification not found with id: %d".formatted(notificationId)));
        delivery.markOpened();
        userNotificationRepository.merge(delivery);
        return NotificationResponse.fromDelivery(delivery.getId(), delivery.getNotification(), delivery.isRead());
    }

    @Transactional
    public NotificationSummaryResponse markRead(String username, Long notificationId) {
        var delivery = requireDelivery(username, notificationId);
        delivery.markRead();
        userNotificationRepository.merge(delivery);
        return NotificationSummaryResponse.from(delivery.getNotification(), delivery.isRead());
    }

    @Transactional
    public NotificationSummaryResponse markUnread(String username, Long notificationId) {
        var delivery = requireDelivery(username, notificationId);
        delivery.markUnread();
        userNotificationRepository.merge(delivery);
        return NotificationSummaryResponse.from(delivery.getNotification(), delivery.isRead());
    }

    private UserNotification requireDelivery(String username, Long notificationId) {
        var user = requireActiveUser(username);
        return userNotificationRepository.findByUserAndNotificationId(user, notificationId)
                                         .orElseThrow(() -> new NotFoundException("Notification not found with id: %d".formatted(notificationId)));
    }

    private User requireActiveUser(String username) {
        return userRepository.findActiveByUsername(username)
                             .orElseThrow(() -> new NotFoundException("User not found"));
    }
}
