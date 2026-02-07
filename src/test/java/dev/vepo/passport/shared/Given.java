package dev.vepo.passport.shared;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.vepo.passport.auth.JwtGenerator;
import dev.vepo.passport.model.Profile;
import dev.vepo.passport.model.User;
import dev.vepo.passport.shared.security.PasswordEncoder;
import dev.vepo.passport.user.UserRepository;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.restassured.http.Header;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.persistence.EntityManager;

public class Given {
    private static final Logger logger = LoggerFactory.getLogger(Given.class);

    public static class UserBuilder {
        private Long id;
        private String email;
        private String name;
        private String username;
        private String password;
        private Set<Profile> profiles;
        private boolean deleted;

        private UserBuilder() {
            this.id = null;
            this.email = null;
            this.name = null;
            this.username = null;
            this.password = null;
            this.profiles = new HashSet<>();
            this.deleted = false;
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

        public UserBuilder withDeleted(boolean deleted) {
            this.deleted = deleted;
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
            return new User(id, username, name, email, inject(PasswordEncoder.class).hashPassword(password), profiles, deleted);
        }

        public GivenUser persist() {
            return new GivenUser(withTransaction(() -> inject(UserRepository.class).save(build())));
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
    }

    public static UserBuilder user() {
        return new UserBuilder();
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

    public static void withTransaction(Runnable block) {
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

    public static <T> T withTransaction(Supplier<T> block) {
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

    public static void cleanup() {
        withTransaction(() -> {
            var em = inject(EntityManager.class);
            em.createQuery("DELETE FROM ResetPasswordToken");
            em.createQuery("DELETE FROM User");
            em.createQuery("DELETE FROM Profile");
            em.createQuery("DELETE FROM Role");
        });
    }

}
