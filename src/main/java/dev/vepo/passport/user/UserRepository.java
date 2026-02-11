package dev.vepo.passport.user;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.vepo.passport.model.ResetPasswordToken;
import dev.vepo.passport.model.User;
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
public class UserRepository {
    public class UserSearchCriteria {
        private String name;
        private String email;
        private List<Long> profileIds;
        private List<Long> roleIds;
        private Boolean disabled;

        public UserSearchCriteria name(String name) {
            this.name = name;
            return this;
        }

        public UserSearchCriteria email(String email) {
            this.email = email;
            return this;
        }

        public UserSearchCriteria profileIds(List<Long> profileIds) {
            this.profileIds = profileIds;
            return this;
        }

        public UserSearchCriteria roleIds(List<Long> roleIds) {
            this.roleIds = roleIds;
            return this;
        }

        public UserSearchCriteria disabled(Boolean disabled) {
            this.disabled = disabled;
            return this;
        }

        public List<User> execute() {
            return search(this);
        }
    }

    private static final Logger logger = LoggerFactory.getLogger(UserRepository.class);

    private EntityManager entityManager;

    @Inject
    public UserRepository(EntityManager entityManager) {
        this.entityManager = entityManager;

    }

    public Optional<User> findByEmail(String email) {
        return entityManager.createQuery("FROM User WHERE email = :email", User.class)
                            .setParameter("email", email)
                            .getResultStream()
                            .findFirst();
    }

    public Optional<User> findActiveByEmail(String email) {
        return entityManager.createQuery("FROM User WHERE email = :email AND disabled = false", User.class)
                            .setParameter("email", email)
                            .getResultStream()
                            .findFirst();
    }

    public Optional<User> findActiveByUsername(String username) {
        return entityManager.createQuery("FROM User WHERE username = :username AND disabled = false", User.class)
                            .setParameter("username", username)
                            .getResultStream()
                            .findFirst();
    }

    public Optional<User> findActiveByUsernameAndPassword(String username, String encodedPassword) {
        return entityManager.createQuery("""
                                         FROM User
                                         WHERE username = :username AND
                                               encodedPassword = :encodedPassword AND
                                               disabled = false
                                         """, User.class)
                            .setParameter("username", username)
                            .setParameter("encodedPassword", encodedPassword)
                            .getResultStream()
                            .findFirst();
    }

    public Optional<User> findByUsername(String username) {
        return entityManager.createQuery("FROM User WHERE username = :username", User.class)
                            .setParameter("username", username)
                            .getResultStream()
                            .findFirst();
    }

    public List<User> findByUsernameOrEmail(String username, String email) {
        return entityManager.createQuery("FROM User WHERE username = :username OR email = :email", User.class)
                            .setParameter("username", username)
                            .setParameter("email", email)
                            .getResultStream()
                            .toList();
    }

    public Optional<User> findById(Long id) {
        return entityManager.createQuery("FROM User WHERE id = :id", User.class)
                            .setParameter("id", id)
                            .getResultStream()
                            .findFirst();
    }

    public User save(User user) {
        try {
            entityManager.persist(user);
            return user;
        } catch (PersistenceException e) {
            logger.error("Failed to save user with email: {}", user.getEmail(), e);
            throw new RepositoryException("Failed to save user", e);
        }
    }

    public ResetPasswordToken save(ResetPasswordToken token) {
        try {
            entityManager.persist(token);
            logger.debug("Persisted reset password token for user ID: {}", token.getUser().getId());
            return token;
        } catch (PersistenceException e) {
            logger.error("Failed to save reset password token", e);
            throw new RepositoryException("Failed to save reset password token", e);
        }
    }

    public UserSearchCriteria search() {
        return new UserSearchCriteria();
    }

    public List<User> search(UserSearchCriteria criteria) {
        logger.info("Searching for users...");
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<User> criteriaQuery = criteriaBuilder.createQuery(User.class);
        Root<User> userRoot = criteriaQuery.from(User.class);

        List<Predicate> predicates = buildSearchPredicates(criteria, criteriaBuilder, criteriaQuery, userRoot);

        if (!predicates.isEmpty()) {
            criteriaQuery.where(criteriaBuilder.and(predicates.toArray(new Predicate[0])));
        }

        criteriaQuery.orderBy(criteriaBuilder.asc(userRoot.get("name")));

        try {
            return entityManager.createQuery(criteriaQuery)
                                .getResultStream()
                                .toList();
        } catch (PersistenceException e) {
            logger.error("Failed to execute user search with criteria: {}", criteria, e);
            throw new RepositoryException("Failed to search users", e);
        }
    }

    public Optional<ResetPasswordToken> findValidResetPasswordTokenByUserId(Long userId) {
        return entityManager.createQuery("""
                                         FROM ResetPasswordToken
                                         WHERE user.id = :userId AND
                                               requestedAt > :expire_threshold AND
                                               used <> true
                                         """, ResetPasswordToken.class)
                            .setParameter("userId", userId)
                            .setParameter("expire_threshold", Instant.now()
                                                                     .minus(Duration.ofDays(1)))
                            .getResultStream()
                            .findFirst();
    }

    public Optional<ResetPasswordToken> findValidResetPasswordTokenByTokenAndPassword(String token, String recoveryPassword) {
        return entityManager.createQuery("""
                                         FROM ResetPasswordToken
                                         WHERE token = :token AND
                                               encodedPassword = :encodedPassword AND
                                               requestedAt > :expire_threshold AND
                                               used = false AND
                                               user.disabled = false
                                         """, ResetPasswordToken.class)
                            .setParameter("token", token)
                            .setParameter("encodedPassword", recoveryPassword)
                            .setParameter("expire_threshold", Instant.now()
                                                                     .minus(Duration.ofDays(1)))
                            .getResultStream()
                            .findFirst();
    }

    private List<Predicate> buildSearchPredicates(UserSearchCriteria criteria,
                                                  CriteriaBuilder criteriaBuilder,
                                                  CriteriaQuery<User> criteriaQuery,
                                                  Root<User> userRoot) {
        List<Predicate> predicates = new ArrayList<>();

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

        if (Objects.nonNull(criteria.email) && !criteria.email.isBlank()) {
            predicates.add(criteriaBuilder.like(criteriaBuilder.lower(userRoot.get("email")),
                                                "%%%s%%".formatted(criteria.email.toLowerCase())));
        }

        if (Objects.nonNull(criteria.profileIds) && !criteria.profileIds.isEmpty()) {
            predicates.add(createProfilePredicate(criteria, criteriaBuilder, criteriaQuery, userRoot));
        }

        if (Objects.nonNull(criteria.roleIds) && !criteria.roleIds.isEmpty()) {
            predicates.add(createRolePredicate(criteria, criteriaBuilder, criteriaQuery, userRoot));
        }

        return predicates;
    }

    private Predicate createRolePredicate(UserSearchCriteria criteria, CriteriaBuilder criteriaBuilder, CriteriaQuery<User> criteriaQuery,
                                          Root<User> userRoot) {
        var subquery = criteriaQuery.subquery(Long.class);
        var subUser = subquery.correlate(userRoot);
        var subProfileJoin = subUser.join("profiles");
        var subRoleJoin = subProfileJoin.join("roles");

        subquery.select(subUser.get("id"))
                .where(subRoleJoin.get("id").in(criteria.roleIds));

        return criteriaBuilder.exists(subquery);
    }

    private Predicate createProfilePredicate(UserSearchCriteria criteria,
                                             CriteriaBuilder criteriaBuilder,
                                             CriteriaQuery<User> criteriaQuery,
                                             Root<User> userRoot) {
        var subquery = criteriaQuery.subquery(Long.class);
        var subUser = subquery.correlate(userRoot);
        var subProfileJoin = subUser.join("profiles");

        subquery.select(subUser.get("id"))
                .where(subProfileJoin.get("id").in(criteria.profileIds));

        return criteriaBuilder.exists(subquery);
    }
}