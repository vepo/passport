package dev.vepo.passport.auth;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.notNullValue;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

@QuarkusTest
class LoginEndpointTest {
    @Test
    @DisplayName("Test sucessful login")
    void successfulLoginTest() {
        given().contentType(ContentType.JSON)
               .body("""
                     {
                        "email": "sysadmin@passport.vepo.dev",
                        "password": "qwas1234"
                     }
                     """)
               .when()
               .post("/api/auth/login")
               .then()
               .statusCode(HttpStatus.SC_OK)
               .body("token", notNullValue());
    }

    @Test
    @DisplayName("Test wrong password login")
    void wrongPasswordLogin() {
        given().contentType(ContentType.JSON)
               .body("""
                     {
                        "email": "sysadmin@passport.vepo.dev",
                        "password": "qwas12341"
                     }
                     """)
               .when()
               .post("/api/auth/login")
               .then()
               .statusCode(HttpStatus.SC_UNAUTHORIZED)
               .body("status", is(HttpStatus.SC_UNAUTHORIZED))
               .body("message", is("Invalid credentials!"));
    }

    @Test
    @DisplayName("Test wrong email login")
    void wrongUsernameLogin() {
        given().contentType(ContentType.JSON)
               .body("""
                     {
                        "email": "admin@passport.vepo.dev",
                        "password": "qwas1234"
                     }
                     """)
               .when()
               .post("/api/auth/login")
               .then()
               .statusCode(HttpStatus.SC_UNAUTHORIZED)
               .body("status", is(HttpStatus.SC_UNAUTHORIZED))
               .body("message", is("Invalid credentials!"));
    }

    @Test
    @DisplayName("Test no email login")
    void noUsernameLogin() {
        given().contentType(ContentType.JSON)
               .body("""
                     {
                        "password": "qwas1234"
                     }
                     """)
               .when()
               .post("/api/auth/login")
               .then()
               .statusCode(HttpStatus.SC_BAD_REQUEST)
               .body("status", is(HttpStatus.SC_BAD_REQUEST))
               .body("title", is("Constraint Violation"))
               .body("violations[0].field", is("login.request.email"))
               .body("violations[0].message", is("Email must not be empty!"));
    }
}
