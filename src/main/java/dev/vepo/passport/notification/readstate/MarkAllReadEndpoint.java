package dev.vepo.passport.notification.readstate;

import dev.vepo.passport.notification.MarkAllReadResponse;
import dev.vepo.passport.notification.NotificationService;
import io.quarkus.security.Authenticated;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.SecurityContext;

@ApplicationScoped
@Path("/notifications/read-all")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class MarkAllReadEndpoint {

    private final NotificationService notificationService;

    @Inject
    public MarkAllReadEndpoint(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PATCH
    public MarkAllReadResponse markAllRead(@Context SecurityContext securityContext) {
        return notificationService.markAllRead(securityContext.getUserPrincipal().getName());
    }
}
