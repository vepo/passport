package dev.vepo.passport.shared.security;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

import java.util.Map;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.vepo.passport.shared.Given;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.restassured.http.ContentType;

@QuarkusTest
@TestProfile(AuthRateLimitFilterTest.RateLimitEnabledProfile.class)
@DisplayName("Auth rate limit filter")
class AuthRateLimitFilterTest {

    private static final String LOGIN_ENDPOINT = "/api/auth/login";
    private static final String ADMIN_EMAIL = "rate-limit@passport.vepo.dev";
    private static final String ADMIN_PASSWORD = "qwas1234";

    @BeforeEach
    void cleanup() {
        Given.cleanup();
        Given.user()
             .withEmail(ADMIN_EMAIL)
             .withName("Rate Limit")
             .withUsername("rate-limit-user")
             .withPassword(ADMIN_PASSWORD)
             .persist();
    }

    @Test
    @DisplayName("Should reject authentication requests after the configured limit")
    void login_ExceedingRateLimit_ReturnsTooManyRequests() {
        var clientIp = "203.0.113.10";

        for (var attempt = 0; attempt < 2; attempt++) {
            given().header("X-Forwarded-For", clientIp)
                   .contentType(ContentType.JSON)
                   .body(Map.of("email", ADMIN_EMAIL, "password", "wrong-password"))
                   .when()
                   .post(LOGIN_ENDPOINT)
                   .then()
                   .statusCode(HttpStatus.SC_UNAUTHORIZED);
        }

        given().header("X-Forwarded-For", clientIp)
               .contentType(ContentType.JSON)
               .body(Map.of("email", ADMIN_EMAIL, "password", ADMIN_PASSWORD))
               .when()
               .post(LOGIN_ENDPOINT)
               .then()
               .statusCode(HttpStatus.SC_TOO_MANY_REQUESTS)
               .body(is("Too many authentication requests"));
    }

    public static class RateLimitEnabledProfile implements QuarkusTestProfile {

        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("passport.auth.rate-limit.enabled", "true",
                          "passport.auth.rate-limit.max-requests", "2",
                          "passport.auth.rate-limit.window", "PT1M");
        }

    }

}
