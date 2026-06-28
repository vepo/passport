package dev.vepo.passport.notification.readstate;

import dev.vepo.passport.notification.NotificationService;
import dev.vepo.passport.notification.NotificationSummaryResponse;
import io.quarkus.security.Authenticated;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.SecurityContext;

@ApplicationScoped
@Path("/notifications/{notificationId}")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class NotificationReadStateEndpoint {

    private final NotificationService notificationService;

    @Inject
    public NotificationReadStateEndpoint(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PATCH
    @Path("/read")
    public NotificationSummaryResponse markRead(@Context SecurityContext securityContext,
                                                @PathParam("notificationId") Long notificationId) {
        return notificationService.markRead(securityContext.getUserPrincipal().getName(), notificationId);
    }

    @PATCH
    @Path("/unread")
    public NotificationSummaryResponse markUnread(@Context SecurityContext securityContext,
                                                  @PathParam("notificationId") Long notificationId) {
        return notificationService.markUnread(securityContext.getUserPrincipal().getName(), notificationId);
    }
}
