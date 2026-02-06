package dev.vepo.passport.profile;

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
}
