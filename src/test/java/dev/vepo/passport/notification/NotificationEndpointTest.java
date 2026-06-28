package dev.vepo.passport.notification;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.vepo.passport.shared.Given;
import dev.vepo.passport.shared.security.InternalServiceKeyFilter;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

@QuarkusTest
@DisplayName("Notification API Endpoint Tests")
class NotificationEndpointTest {

    private static final String SERVICE_KEY = "test-service-key";

    @BeforeEach
    void cleanup() {
        Given.cleanup();
    }

    @Test
    @DisplayName("Should reject internal notification without service key")
    void createInternalNotification_WithoutServiceKey_ReturnsUnauthorized() {
        given().contentType(ContentType.JSON)
               .body("""
                     {
                       "sourceService": "engage",
                       "sourceType": "video_sync",
                       "engageChannelId": 1,
                       "title": "Sync",
                       "description": "Test",
                       "report": "{}",
                       "items": []
                     }
                     """)
               .when().post("/api/internal/notifications")
               .then()
               .statusCode(HttpStatus.SC_UNAUTHORIZED);
    }

    @Test
    @DisplayName("Should fan-out notification to channel followers")
    void createInternalNotification_ForFollowers_DeliversToUser() {
        var user = Given.user()
                        .withUsername("notify-user")
                        .withEmail("notify@passport.vepo.dev")
                        .withName("Notify User")
                        .withPassword("password123")
                        .persist();

        given().header(user.authenticated())
               .contentType(ContentType.JSON)
               .body("{\"engageChannelId\": 1}")
               .when().post("/api/channel-follows")
               .then()
               .statusCode(HttpStatus.SC_CREATED);

        given().header(InternalServiceKeyFilter.SERVICE_KEY_HEADER, SERVICE_KEY)
               .contentType(ContentType.JSON)
               .body("""
                     {
                       "sourceService": "engage",
                       "sourceType": "video_sync",
                       "engageChannelId": 1,
                       "title": "Sincronização de vídeos",
                       "description": "Canal UC teste",
                       "report": "{\\"status\\":\\"ok\\"}",
                       "items": [
                         {
                           "title": "youtube.search.list",
                           "description": "1 página",
                           "report": "{\\"count\\":1}"
                         }
                       ]
                     }
                     """)
               .when().post("/api/internal/notifications")
               .then()
               .statusCode(HttpStatus.SC_CREATED)
               .body("title", is("Sincronização de vídeos"));

        given().header(user.authenticated())
               .when().get("/api/notifications")
               .then()
               .statusCode(HttpStatus.SC_OK)
               .body("$", hasSize(1))
               .body("[0].read", is(false))
               .body("[0].title", is("Sincronização de vídeos"));

        given().header(user.authenticated())
               .when().get("/api/notifications/unread-count")
               .then()
               .statusCode(HttpStatus.SC_OK)
               .body("count", equalTo(1));
    }

    @Test
    @DisplayName("Should mark notification read on open and allow unread")
    void openNotification_MarksReadAndCanMarkUnread() {
        var user = Given.user()
                        .withUsername("read-user")
                        .withEmail("read@passport.vepo.dev")
                        .withName("Read User")
                        .withPassword("password123")
                        .persist();

        given().header(user.authenticated())
               .contentType(ContentType.JSON)
               .body("{\"engageChannelId\": 2}")
               .post("/api/channel-follows");

        var notificationId = given().header(InternalServiceKeyFilter.SERVICE_KEY_HEADER, SERVICE_KEY)
                                    .contentType(ContentType.JSON)
                                    .body("""
                                          {
                                            "sourceService": "engage",
                                            "sourceType": "comment_sync",
                                            "engageChannelId": 2,
                                            "title": "Comentários",
                                            "description": "Sync",
                                            "report": "{}",
                                            "items": []
                                          }
                                          """)
                                    .post("/api/internal/notifications")
                                    .then()
                                    .statusCode(HttpStatus.SC_CREATED)
                                    .extract()
                                    .path("id");

        given().header(user.authenticated())
               .when().get("/api/notifications/%d".formatted(notificationId))
               .then()
               .statusCode(HttpStatus.SC_OK)
               .body("read", is(true));

        given().header(user.authenticated())
               .when().patch("/api/notifications/%d/unread".formatted(notificationId))
               .then()
               .statusCode(HttpStatus.SC_OK)
               .body("read", is(false));

        given().header(user.authenticated())
               .when().patch("/api/notifications/%d/read".formatted(notificationId))
               .then()
               .statusCode(HttpStatus.SC_OK)
               .body("read", is(true));
    }

    @Test
    @DisplayName("Should list all sync reports for an Engage channel")
    void listByEngageChannel_ReturnsChannelReports() {
        Given.profile().withName("Engage Manager").withRole("engage.admin").persist();
        var admin = Given.user()
                         .withUsername("engage-admin")
                         .withEmail("engage-admin@passport.vepo.dev")
                         .withName("Engage Admin")
                         .withPassword("password123")
                         .withProfile("Engage Manager")
                         .persist();

        given().header(InternalServiceKeyFilter.SERVICE_KEY_HEADER, SERVICE_KEY)
               .contentType(ContentType.JSON)
               .body("""
                     {
                       "sourceService": "engage",
                       "sourceType": "video_sync",
                       "engageChannelId": 9,
                       "title": "Relatório do canal",
                       "description": "Sync",
                       "report": "{}",
                       "items": []
                     }
                     """)
               .when().post("/api/internal/notifications")
               .then()
               .statusCode(HttpStatus.SC_CREATED);

        given().header(admin.authenticated())
               .when().get("/api/notifications/by-channel/9")
               .then()
               .statusCode(HttpStatus.SC_OK)
               .body("$", hasSize(1))
               .body("[0].title", is("Relatório do canal"));
    }
}
