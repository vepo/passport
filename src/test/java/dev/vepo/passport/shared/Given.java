package dev.vepo.passport.shared;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Objects;

import dev.vepo.passport.auth.JwtGenerator;
import dev.vepo.passport.user.User;
import dev.vepo.passport.user.UserRepository;
import io.restassured.http.Header;
import jakarta.enterprise.inject.spi.CDI;

public class Given {

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

}
