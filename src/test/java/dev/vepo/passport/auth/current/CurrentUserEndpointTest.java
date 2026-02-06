package dev.vepo.passport.auth.current;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.is;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.vepo.passport.shared.Given;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class CurrentUserEndpointTest {
    @Test
    @DisplayName("Test if not authenticated")
    void notAuthenticatedTest() {
        when().get("/api/auth/me")
              .then()
              .statusCode(HttpStatus.SC_UNAUTHORIZED);
    }

    @Test
    @DisplayName("Test if authenticated")
    void authenticatedTest() {
        var user = Given.user("sysadmin@passport.vepo.dev");
        given().header(user.authenticated())
               .when().get("/api/auth/me")
               .then()
               .statusCode(HttpStatus.SC_OK)
               .body("username", is(user.username()));
    }

    // @Test
    // @DisplayName("Test if authenticated")
    // void notRegisteredUserTest() {
    // var user = Given.user(new User("not-registered", "Not Registered",
    // "not-registerd@passport.vepo.dev", "",
    // Set.of(Role.ADMIN, Role.PROJECT_MANAGER, Role.USER)));
    // given().header(user.authenticated())
    // .when().get("/api/auth/me")
    // .then()
    // .statusCode(HttpStatus.SC_UNAUTHORIZED);
    // }
}
