package dev.vepo.passport.profile;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.vepo.passport.model.Profile;
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
public class ProfileRepository {
    public class ProfileSearchCriteria {
        private String name;
        private List<Long> roleIds;
        private Boolean disabled;

        public ProfileSearchCriteria name(String name) {
            this.name = name;
            return this;
        }

        public ProfileSearchCriteria roleIds(List<Long> roleIds) {
            this.roleIds = roleIds;
            return this;
        }

        public ProfileSearchCriteria disabled(Boolean disabled) {
            this.disabled = disabled;
            return this;
        }

        public List<Profile> execute() {
            return search(this);
        }
    }

    private static final Logger logger = LoggerFactory.getLogger(ProfileRepository.class);

    private final EntityManager entityManager;

    @Inject
    public ProfileRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public Set<Profile> findAll() {
        return this.entityManager.createQuery("FROM Profile WHERE disabled = false", Profile.class)
                                 .getResultStream()
                                 .collect(Collectors.toSet());
    }

    public Set<Profile> findByIds(Set<Long> profileIds) {
        return this.entityManager.createQuery("FROM Profile WHERE id IN :profileIds", Profile.class)
                                 .setParameter("profileIds", profileIds)
                                 .getResultStream()
                                 .collect(Collectors.toSet());
    }

    public Optional<Profile> findByName(String name) {
        return this.entityManager.createQuery("FROM Profile WHERE lower(name) = :name", Profile.class)
                                 .setParameter("name", name.toLowerCase())
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

    public ProfileSearchCriteria search() {
        return new ProfileSearchCriteria();
    }

    public List<Profile> search(ProfileSearchCriteria criteria) {
        logger.info("Searching for users...");
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Profile> criteriaQuery = criteriaBuilder.createQuery(Profile.class);
        Root<Profile> profileRoot = criteriaQuery.from(Profile.class);

        List<Predicate> predicates = buildSearchPredicates(criteria, criteriaBuilder, criteriaQuery, profileRoot);

        if (!predicates.isEmpty()) {
            criteriaQuery.where(criteriaBuilder.and(predicates.toArray(new Predicate[0])));
        }

        criteriaQuery.orderBy(criteriaBuilder.asc(profileRoot.get("name")));

        try {
            return entityManager.createQuery(criteriaQuery)
                                .getResultStream()
                                .toList();
        } catch (PersistenceException e) {
            logger.error("Failed to execute user search with criteria: {}", criteria, e);
            throw new RepositoryException("Failed to search users", e);
        }
    }

    private List<Predicate> buildSearchPredicates(ProfileSearchCriteria criteria,
                                                  CriteriaBuilder criteriaBuilder,
                                                  CriteriaQuery<Profile> criteriaQuery,
                                                  Root<Profile> userRoot) {
        var predicates = new ArrayList<Predicate>();

        // Always exclude disabled users
        if (Objects.nonNull(criteria.disabled)) {
            if (criteria.disabled) {
                predicates.add(criteriaBuilder.isTrue(userRoot.get("disabled")));
            } else {
                predicates.add(criteriaBuilder.isFalse(userRoot.get("disabled")));
            }
        }

        if (Objects.nonNull(criteria.name) && !criteria.name.isBlank()) {
            predicates.add(criteriaBuilder.like(criteriaBuilder.lower(userRoot.get("name")),
                                                "%%%s%%".formatted(criteria.name.toLowerCase())));
        }

        if (Objects.nonNull(criteria.roleIds) && !criteria.roleIds.isEmpty()) {
            predicates.add(createRolePredicate(criteria, criteriaBuilder, criteriaQuery, userRoot));
        }

        return predicates;
    }

    private Predicate createRolePredicate(ProfileSearchCriteria criteria, CriteriaBuilder criteriaBuilder, CriteriaQuery<Profile> criteriaQuery,
                                          Root<Profile> userRoot) {
        var subquery = criteriaQuery.subquery(Long.class);
        var subUser = subquery.correlate(userRoot);
        var subRoleJoin = subUser.join("roles");

        subquery.select(subUser.get("id"))
                .where(subRoleJoin.get("id").in(criteria.roleIds));

        return criteriaBuilder.exists(subquery);
    }
}
