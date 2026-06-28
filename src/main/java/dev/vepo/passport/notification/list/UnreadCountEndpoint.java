package dev.vepo.passport.notification.list;

import dev.vepo.passport.notification.NotificationService;
import dev.vepo.passport.notification.UnreadCountResponse;
import io.quarkus.security.Authenticated;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.SecurityContext;

@ApplicationScoped
@Path("/notifications/unread-count")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class UnreadCountEndpoint {

    private final NotificationService notificationService;

    @Inject
    public UnreadCountEndpoint(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GET
    public UnreadCountResponse unreadCount(@Context SecurityContext securityContext) {
        return new UnreadCountResponse(notificationService.countUnreadForUser(securityContext.getUserPrincipal().getName()));
    }
}
