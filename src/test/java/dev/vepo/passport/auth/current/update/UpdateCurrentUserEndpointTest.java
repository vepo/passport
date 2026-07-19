package dev.vepo.passport.auth.current.update;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.vepo.passport.shared.Given;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

@QuarkusTest
@DisplayName("Update current user API")
class UpdateCurrentUserEndpointTest {

    @BeforeEach
    void cleanup() {
        Given.cleanup();
    }

    @Test
    @DisplayName("Should update own name and email when authenticated")
    void shouldUpdateOwnNameAndEmailWhenAuthenticated() {
        var user = Given.user()
                        .withEmail("before@passport.vepo.dev")
                        .withName("Before Name")
                        .withUsername("self-upd")
                        .withPassword("encryptedPassword123")
                        .persist();

        given().header(user.authenticated())
               .contentType(ContentType.JSON)
               .body("""
                     {"name":"After Name","email":"after@passport.vepo.dev","description":"Bio text"}
                     """)
               .when()
               .put("/api/auth/me")
               .then()
               .statusCode(HttpStatus.SC_OK)
               .body("username", is("self-upd"))
               .body("name", is("After Name"))
               .body("email", is("after@passport.vepo.dev"))
               .body("description", is("Bio text"));
    }

    @Test
    @DisplayName("Should reject email already used by another user")
    void shouldRejectEmailAlreadyUsedByAnotherUser() {
        Given.user()
             .withEmail("taken@passport.vepo.dev")
             .withName("Taken")
             .withUsername("taken")
             .withPassword("encryptedPassword123")
             .persist();

        var user = Given.user()
                        .withEmail("mine@passport.vepo.dev")
                        .withName("Mine")
                        .withUsername("mine")
                        .withPassword("encryptedPassword123")
                        .persist();

        given().header(user.authenticated())
               .contentType(ContentType.JSON)
               .body("""
                     {"name":"Mine","email":"taken@passport.vepo.dev"}
                     """)
               .when()
               .put("/api/auth/me")
               .then()
               .statusCode(HttpStatus.SC_CONFLICT);
    }

    @Test
    @DisplayName("Should return UNAUTHORIZED without authentication")
    void shouldReturnUnauthorizedWithoutAuthentication() {
        given().contentType(ContentType.JSON)
               .body("""
                     {"name":"X","email":"x@passport.vepo.dev"}
                     """)
               .when()
               .put("/api/auth/me")
               .then()
               .statusCode(HttpStatus.SC_UNAUTHORIZED);
    }
}
