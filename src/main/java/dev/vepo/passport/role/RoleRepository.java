package dev.vepo.passport.role;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.vepo.passport.model.Role;
import dev.vepo.passport.shared.exception.RepositoryException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

@ApplicationScoped
public class RoleRepository {
    public class RoleSearchCriteria {
        private String name;

        public RoleSearchCriteria name(String name) {
            this.name = name;
            return this;
        }

        public List<Role> execute() {
            return search(this);
        }
    }

    private static final Logger logger = LoggerFactory.getLogger(RoleRepository.class);

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

    public Optional<Role> findById(long roleId) {
        return this.entityManager.createQuery("FROM Role WHERE id = :roleId", Role.class)
                                 .setParameter("roleId", roleId)
                                 .getResultStream()
                                 .findFirst();
    }

    public RoleSearchCriteria search() {
        return new RoleSearchCriteria();
    }

    public List<Role> search(RoleSearchCriteria criteria) {
        logger.info("Searching for users...");
        var criteriaBuilder = entityManager.getCriteriaBuilder();
        var criteriaQuery = criteriaBuilder.createQuery(Role.class);
        var roleRoot = criteriaQuery.from(Role.class);

        var predicates = buildSearchPredicates(criteria, criteriaBuilder, criteriaQuery, roleRoot);

        if (!predicates.isEmpty()) {
            criteriaQuery.where(criteriaBuilder.and(predicates.toArray(new Predicate[0])));
        }

        criteriaQuery.orderBy(criteriaBuilder.asc(roleRoot.get("name")));

        try {
            return entityManager.createQuery(criteriaQuery)
                                .getResultStream()
                                .toList();
        } catch (PersistenceException e) {
            logger.error("Failed to execute user search with criteria: {}", criteria, e);
            throw new RepositoryException("Failed to search users", e);
        }
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

    private List<Predicate> buildSearchPredicates(RoleSearchCriteria criteria,
                                                  CriteriaBuilder criteriaBuilder,
                                                  CriteriaQuery<Role> criteriaQuery,
                                                  Root<Role> roleRoot) {
        var predicates = new ArrayList<Predicate>();

        if (Objects.nonNull(criteria.name) && !criteria.name.isBlank()) {
            predicates.add(criteriaBuilder.like(criteriaBuilder.lower(roleRoot.get("name")),
                                                "%%%s%%".formatted(criteria.name.toLowerCase())));
        }
        return predicates;
    }

    public void delete(long roleId) {
        // First remove all associations from the join table
        entityManager.createNativeQuery("DELETE FROM tb_profile_roles WHERE role_id = :roleId")
                     .setParameter("roleId", roleId)
                     .executeUpdate();
        entityManager.createQuery("DELETE FROM Role WHERE id = :roleId")
                     .setParameter("roleId", roleId)
                     .executeUpdate();
    }
}