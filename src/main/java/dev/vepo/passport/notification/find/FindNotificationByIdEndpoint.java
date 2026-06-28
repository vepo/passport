package dev.vepo.passport.notification.find;

import dev.vepo.passport.notification.NotificationResponse;
import dev.vepo.passport.notification.NotificationService;
import io.quarkus.security.Authenticated;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
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
public class FindNotificationByIdEndpoint {

    private final NotificationService notificationService;

    @Inject
    public FindNotificationByIdEndpoint(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GET
    public NotificationResponse find(@Context SecurityContext securityContext, @PathParam("notificationId") Long notificationId) {
        return notificationService.findForUser(securityContext.getUserPrincipal().getName(), notificationId);
    }
}
