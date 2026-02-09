package dev.vepo.passport.shared;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.time.Instant;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.vepo.passport.auth.JwtGenerator;
import dev.vepo.passport.model.Profile;
import dev.vepo.passport.model.ResetPasswordToken;
import dev.vepo.passport.model.Role;
import dev.vepo.passport.model.User;
import dev.vepo.passport.profile.ProfileRepository;
import dev.vepo.passport.role.RoleRepository;
import dev.vepo.passport.shared.security.PasswordEncoder;
import dev.vepo.passport.shared.security.RequiredRoles;
import dev.vepo.passport.user.UserRepository;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.restassured.http.Header;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Status;

public class Given {
    public static class UserBuilder {
        private Long id;
        private String email;
        private String name;
        private String username;
        private String password;
        private Set<String> profiles;
        private boolean disabled;

        private UserBuilder() {
            this.id = null;
            this.email = null;
            this.name = null;
            this.username = null;
            this.password = null;
            this.profiles = new HashSet<>();
            this.disabled = false;
        }

        public UserBuilder withId(Long id) {
            this.id = id;
            return this;
        }

        public UserBuilder withEmail(String email) {
            this.email = email;
            return this;
        }

        public UserBuilder withName(String name) {
            this.name = name;
            return this;
        }

        public UserBuilder withUsername(String username) {
            this.username = username;
            return this;
        }

        public UserBuilder withPassword(String password) {
            this.password = password;
            return this;
        }

        public UserBuilder withDisabled(boolean disabled) {
            this.disabled = disabled;
            return this;
        }

        public UserBuilder withProfile(String profile) {
            this.profiles.add(profile);
            return this;
        }

        public Header authenticated() {
            return new Header("Authorization", "Bearer %s".formatted(inject(JwtGenerator.class).generate(build())));
        }

        public User build() {
            Objects.requireNonNull(username, "'username' cannot be null!");
            Objects.requireNonNull(name, "'name' cannot be null!");
            Objects.requireNonNull(email, "'email' cannot be null!");
            Objects.requireNonNull(password, "'password' cannot be null!");
            return new User(id, username, name, email, inject(PasswordEncoder.class).hashPassword(password), asProfiles(), disabled);
        }

        public GivenUser persist() {
            return new GivenUser(withTransaction(() -> inject(UserRepository.class).save(build())));
        }

        private Set<Profile> asProfiles() {
            return this.profiles.stream()
                                .map(profile -> inject(ProfileRepository.class).findByName(profile)
                                                                               .orElseGet(() -> inject(ProfileRepository.class).save(new Profile(profile))))
                                .collect(Collectors.toSet());
        }
    }

    public static class GivenUser {
        private final User user;

        public GivenUser(User user) {
            this.user = user;
        }

        public Header authenticated() {
            return new Header("Authorization", "Bearer %s".formatted(inject(JwtGenerator.class).generate(user)));
        }

        public String username() {
            return user.getUsername();
        }

        public Long id() {
            return user.getId();
        }

        public String email() {
            return user.getEmail();
        }

        public String name() {
            return user.getName();
        }

        public User user() {
            return this.user;
        }
    }

    public static class ResetPasswordTokenBuilder {
        private User user;
        private String token;
        private String password;
        private boolean used;
        private Instant requestedAt;

        private ResetPasswordTokenBuilder() {
            this.user = null;
            this.token = null;
            this.password = null;
            this.used = false;
            this.requestedAt = null;
        }

        public ResetPasswordTokenBuilder withToken(String token) {
            this.token = token;
            return this;
        }

        public ResetPasswordTokenBuilder withUser(GivenUser user) {
            this.user = user.user;
            return this;
        }

        public ResetPasswordTokenBuilder withPassword(String password) {
            this.password = password;
            return this;
        }

        public ResetPasswordTokenBuilder withUsed(boolean used) {
            this.used = used;
            return this;
        }

        public ResetPasswordTokenBuilder withRequestedAt(Instant requestedAt) {
            this.requestedAt = requestedAt;
            return this;
        }

        public ResetPasswordToken build() {
            return new ResetPasswordToken(null, token, inject(PasswordEncoder.class).hashPassword(password), user, this.used, this.requestedAt);
        }

        public void persist() {
            withTransaction(() -> inject(UserRepository.class).save(build()));
        }
    }

    public static class ProfileBuilder {
        private String name;
        private Set<String> roles;

        private ProfileBuilder() {
            this.name = null;
            this.roles = new HashSet<>();
        }

        public ProfileBuilder withName(String name) {
            this.name = name;
            return this;
        }

        public ProfileBuilder withRole(String role) {
            this.roles.add(role);
            return this;
        }

        public Profile persist() {
            return withTransaction(() -> inject(ProfileRepository.class).save(new Profile(name, asRoles())));
        }

        private Set<Role> asRoles() {
            var roleRepository = inject(RoleRepository.class);
            return this.roles.stream()
                             .map(role -> roleRepository.findByName(role)
                                                        .orElseGet(() -> roleRepository.save(new Role(role))))
                             .collect(Collectors.toSet());
        }
    }

    public static class RoleBuilder {
        private String name;

        private RoleBuilder() {
            this.name = null;
        }

        public RoleBuilder withName(String name) {
            this.name = name;
            return this;
        }

        public Role persist() {
            return withTransaction(() -> inject(RoleRepository.class).save(new Role(name)));
        }
    }

    private static final Logger logger = LoggerFactory.getLogger(Given.class);

    public static UserBuilder user() {
        return new UserBuilder();
    }

    public static ResetPasswordTokenBuilder resetPassword() {
        return new ResetPasswordTokenBuilder();
    }

    public static ProfileBuilder profile() {
        return new ProfileBuilder();
    }

    public static RoleBuilder role() {
        return new RoleBuilder();
    }

    public static GivenUser user(String email) {
        var maybeUser = inject(UserRepository.class).findByEmail(email);
        assertTrue(maybeUser.isPresent(), "User (email=%s) not found!".formatted(email));
        return new GivenUser(maybeUser.get());
    }

    public static <T> T inject(Class<T> clazz) {
        return CDI.current().select(clazz).get();
    }

    public static GivenUser user(User user) {
        if (Objects.isNull(user.getId())) {
            user.setId(999999l);
        }
        return new GivenUser(user);
    }

    public static GivenUser user(long userId) {
        inject(EntityManager.class).clear(); // avoid using cache
        return new GivenUser(inject(UserRepository.class).findById(userId).orElseThrow());
    }

    public static void withTransaction(Runnable block) {
        if (QuarkusTransaction.getStatus() == Status.STATUS_ACTIVE) {
            block.run();
        } else {
            try {
                QuarkusTransaction.begin();
                block.run();
                QuarkusTransaction.commit();
            } catch (Exception e) {
                logger.error("Could not commit transaction!", e);
                QuarkusTransaction.rollback();
                fail("Fail to create transaction!", e);
            }
        }
    }

    public static <T> T withTransaction(Supplier<T> block) {
        if (QuarkusTransaction.getStatus() == Status.STATUS_ACTIVE) {
            return block.get();
        } else {
            try {
                QuarkusTransaction.begin();
                return block.get();
            } catch (Exception e) {
                logger.error("Could not commit transaction!", e);
                QuarkusTransaction.rollback();
                fail("Fail to create transaction!", e);
                return null;
            } finally {
                QuarkusTransaction.commit();
            }
        }
    }

    public static void cleanup() {
        withTransaction(() -> {
            var em = inject(EntityManager.class);
            em.createQuery("DELETE FROM ResetPasswordToken").executeUpdate();
            em.createQuery("DELETE FROM User").executeUpdate();
            em.createQuery("DELETE FROM Profile").executeUpdate();
            em.createQuery("DELETE FROM Role").executeUpdate();
        });
    }

    public static Profile adminProfile() {
        return inject(ProfileRepository.class).findByName("Admin")
                                              .orElseGet(() -> profile().withName("Admin")
                                                                        .withRole(RequiredRoles.ADMIN)
                                                                        .persist());
    }

    public static GivenUser admin() {
        return new GivenUser(inject(UserRepository.class).findActiveByUsername("admin")
                                                         .orElseGet(() -> withTransaction(() -> inject(UserRepository.class).save(new User("admin",
                                                                                                                                           "Admin",
                                                                                                                                           "admin@passport.vepo.dev",
                                                                                                                                           inject(PasswordEncoder.class).hashPassword("qwas1234"),
                                                                                                                                           Set.of(adminProfile()))))));
    }

    public static void clearDatabaseCache() {
        inject(EntityManager.class).clear();
    }
}
