package dev.vepo.passport.auth.current;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.is;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.vepo.passport.shared.Given;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@DisplayName("Current User API Endpoint Tests")
class CurrentUserEndpointTest {
    @BeforeEach
    void cleanup() {
        Given.cleanup();
    }

    @Test
    @DisplayName("Should return UNAUTHORIZED when accessing endpoint without authentication")
    void getCurrentUser_WithoutAuthentication_ReturnsUnauthorized() {
        when().get("/api/auth/me")
              .then()
              .statusCode(HttpStatus.SC_UNAUTHORIZED);
    }

    @Test
    @DisplayName("Should return OK with user details when authenticated with valid user")
    void getCurrentUser_WithValidAuthentication_ReturnsUserDetails() {
        var user = Given.user()
                        .withEmail("active.user@passport.vepo.dev")
                        .withName("Active User")
                        .withUsername("deleted-user")
                        .withPassword("encryptedPassword123")
                        .persist();

        given().header(user.authenticated())
               .when().get("/api/auth/me")
               .then()
               .statusCode(HttpStatus.SC_OK)
               .body("username", is(user.username()));
    }

    @Test
    @DisplayName("Should return UNAUTHORIZED when authenticated with non-existent user")
    void getCurrentUser_WithNonExistentUser_ReturnsUnauthorized() {
        var nonExistentUser = Given.user()
                                   .withId(999L)
                                   .withEmail("non-existent@passport.vepo.dev")
                                   .withName("Non Existent User")
                                   .withUsername("non-existent")
                                   .withPassword("encryptedPassword123")
                                   .authenticated();

        given().header(nonExistentUser)
               .when().get("/api/auth/me")
               .then()
               .statusCode(HttpStatus.SC_UNAUTHORIZED);
    }

    @Test
    @DisplayName("Should return UNAUTHORIZED when authenticated with deleted user")
    void getCurrentUser_WithDeletedUser_ReturnsUnauthorized() {
        var deletedUser = Given.user()
                               .withEmail("deleted.user@passport.vepo.dev")
                               .withName("Deleted User")
                               .withUsername("deleted-user")
                               .withPassword("encryptedPassword123")
                               .withDisabled(true)
                               .persist()
                               .authenticated();

        given().header(deletedUser)
               .when().get("/api/auth/me")
               .then()
               .statusCode(HttpStatus.SC_UNAUTHORIZED);
    }
}