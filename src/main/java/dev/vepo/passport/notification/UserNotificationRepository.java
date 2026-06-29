package dev.vepo.passport.notification;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import dev.vepo.passport.model.User;
import dev.vepo.passport.model.UserNotification;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class UserNotificationRepository {

    private final EntityManager entityManager;

    @Inject
    public UserNotificationRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Transactional
    public UserNotification save(UserNotification userNotification) {
        entityManager.persist(userNotification);
        return userNotification;
    }

    @Transactional
    public UserNotification merge(UserNotification userNotification) {
        return entityManager.merge(userNotification);
    }

    public List<UserNotification> findByUser(User user, Boolean unreadOnly) {
        var query = """
                    FROM UserNotification un
                    JOIN FETCH un.notification n
                    WHERE un.user = :user
                    """;
        if (Boolean.TRUE.equals(unreadOnly)) {
            query += " AND un.read = false";
        }
        query += " ORDER BY n.createdAt DESC";

        return entityManager.createQuery(query, UserNotification.class)
                            .setParameter("user", user)
                            .getResultList();
    }

    public Optional<UserNotification> findByUserAndNotificationId(User user, Long notificationId) {
        return entityManager.createQuery("""
                                         FROM UserNotification un
                                         JOIN FETCH un.notification n
                                         LEFT JOIN FETCH n.items
                                         WHERE un.user = :user AND n.id = :notificationId
                                         """, UserNotification.class)
                            .setParameter("user", user)
                            .setParameter("notificationId", notificationId)
                            .getResultStream()
                            .findFirst();
    }

    public long countUnreadByUser(User user) {
        return entityManager.createQuery("""
                                         SELECT COUNT(un) FROM UserNotification un
                                         WHERE un.user = :user AND un.read = false
                                         """, Long.class)
                            .setParameter("user", user)
                            .getSingleResult();
    }

    @Transactional
    public int markAllReadByUser(User user) {
        return entityManager.createQuery("""
                                         UPDATE UserNotification un
                                         SET un.read = true, un.readAt = :readAt
                                         WHERE un.user = :user AND un.read = false
                                         """)
                            .setParameter("user", user)
                            .setParameter("readAt", Instant.now())
                            .executeUpdate();
    }

    @Transactional
    public int deleteReadOlderThan(Instant readBefore) {
        return entityManager.createQuery("""
                                         DELETE FROM UserNotification un
                                         WHERE un.read = true AND un.readAt < :readBefore
                                         """)
                            .setParameter("readBefore", readBefore)
                            .executeUpdate();
    }
}
