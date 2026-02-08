package dev.vepo.passport.role;

import java.util.List;
import java.util.Optional;

import dev.vepo.passport.model.Role;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

@ApplicationScoped
public class RoleRepository {
    private final EntityManager entityManager;

    @Inject
    public RoleRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public Optional<Role> findByName(String name) {
        return entityManager.createQuery("FROM Role WHERE name = :name", Role.class)
                            .setParameter("name", name)
                            .getResultStream()
                            .findFirst();
    }

    public List<Role> findAll() {
        return entityManager.createQuery("FROM Role", Role.class)
                            .getResultStream()
                            .toList();
    }

    public Role save(Role role) {
        this.entityManager.persist(role);
        return role;
    }
}