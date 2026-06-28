package dev.vepo.passport.notification;

import java.util.Optional;

import dev.vepo.passport.model.Notification;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class NotificationRepository {

    private final EntityManager entityManager;

    @Inject
    public NotificationRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Transactional
    public Notification save(Notification notification) {
        entityManager.persist(notification);
        return notification;
    }

    public Optional<Notification> findById(Long id) {
        return entityManager.createQuery("FROM Notification n LEFT JOIN FETCH n.items WHERE n.id = :id", Notification.class)
                            .setParameter("id", id)
                            .getResultStream()
                            .findFirst();
    }
}
