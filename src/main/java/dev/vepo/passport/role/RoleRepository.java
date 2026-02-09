package dev.vepo.passport.role;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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
        return entityManager.createQuery("FROM Role WHERE lower(name) = :name", Role.class)
                            .setParameter("name", name.toLowerCase())
                            .getResultStream()
                            .findFirst();
    }

    public Set<Role> findByIds(Set<Long> roleIds) {
        return this.entityManager.createQuery("FROM Role WHERE id IN :roleIds", Role.class)
                                 .setParameter("roleIds", roleIds)
                                 .getResultStream()
                                 .collect(Collectors.toSet());
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