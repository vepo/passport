package dev.vepo.passport.auth;

import static io.restassured.RestAssured.given;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

@QuarkusTest
class RequestResetPasswordEndpointTest {
    @Test
    @DisplayName("Test sucessful login")
    void successfulLoginTest() {
        given().contentType(ContentType.JSON)
               .body("""
                     {
                        "email": "sysadmin@passport.vepo.dev"
                     }
                     """)
               .when()
               .post("/api/auth/recovery")
               .then()
               .statusCode(HttpStatus.SC_OK);
    }
}
