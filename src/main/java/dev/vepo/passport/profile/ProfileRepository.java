package dev.vepo.passport.profile;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import dev.vepo.passport.model.Profile;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

@ApplicationScoped
public class ProfileRepository {
    private final EntityManager entityManager;

    @Inject
    public ProfileRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public Set<Profile> findByIds(Set<Long> profileIds) {
        return this.entityManager.createQuery("FROM Profile WHERE id IN :profileIds", Profile.class)
                                 .setParameter("profileIds", profileIds)
                                 .getResultStream()
                                 .collect(Collectors.toSet());
    }

    public Optional<Profile> findByName(String name) {
        return this.entityManager.createQuery("FROM Profile WHERE name = :name", Profile.class)
                                 .setParameter("name", name)
                                 .getResultStream()
                                 .findFirst();
    }

    public Profile save(Profile profile) {
        this.entityManager.persist(profile);
        return profile;
    }

    public Optional<Profile> findById(Long id) {
        return entityManager.createQuery("FROM Profile WHERE id = :id", Profile.class)
                            .setParameter("id", id)
                            .getResultStream()
                            .findFirst();
    }
}
