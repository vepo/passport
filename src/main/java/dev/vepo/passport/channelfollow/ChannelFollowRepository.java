package dev.vepo.passport.channelfollow;

import java.util.List;
import java.util.Optional;

import dev.vepo.passport.model.ChannelFollow;
import dev.vepo.passport.model.User;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class ChannelFollowRepository {

    private final EntityManager entityManager;

    @Inject
    public ChannelFollowRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Transactional
    public ChannelFollow save(ChannelFollow channelFollow) {
        entityManager.persist(channelFollow);
        return channelFollow;
    }

    @Transactional
    public void delete(ChannelFollow channelFollow) {
        if (entityManager.contains(channelFollow)) {
            entityManager.remove(channelFollow);
        } else {
            entityManager.remove(entityManager.merge(channelFollow));
        }
    }

    public List<ChannelFollow> findByUser(User user) {
        return entityManager.createQuery("FROM ChannelFollow cf WHERE cf.user = :user ORDER BY cf.createdAt DESC",
                                         ChannelFollow.class)
                            .setParameter("user", user)
                            .getResultList();
    }

    public List<ChannelFollow> findByEngageChannelId(Long engageChannelId) {
        return entityManager.createQuery("FROM ChannelFollow cf WHERE cf.engageChannelId = :engageChannelId",
                                         ChannelFollow.class)
                            .setParameter("engageChannelId", engageChannelId)
                            .getResultList();
    }

    public Optional<ChannelFollow> findByUserAndEngageChannelId(User user, Long engageChannelId) {
        return entityManager.createQuery("""
                                         FROM ChannelFollow cf
                                         WHERE cf.user = :user AND cf.engageChannelId = :engageChannelId
                                         """, ChannelFollow.class)
                            .setParameter("user", user)
                            .setParameter("engageChannelId", engageChannelId)
                            .getResultStream()
                            .findFirst();
    }

    public boolean existsByUserAndEngageChannelId(User user, Long engageChannelId) {
        return findByUserAndEngageChannelId(user, engageChannelId).isPresent();
    }
}
