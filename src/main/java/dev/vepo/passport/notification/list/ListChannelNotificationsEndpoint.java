package dev.vepo.passport.notification.list;

import java.util.List;

import dev.vepo.passport.notification.NotificationService;
import dev.vepo.passport.notification.NotificationSummaryResponse;
import jakarta.annotation.security.RolesAllowed;
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
@Path("/notifications/by-channel/{engageChannelId}")
@RolesAllowed("engage.admin")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ListChannelNotificationsEndpoint {

    private final NotificationService notificationService;

    @Inject
    public ListChannelNotificationsEndpoint(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GET
    public List<NotificationSummaryResponse> list(@Context SecurityContext securityContext,
                                                  @PathParam("engageChannelId") Long engageChannelId) {
        return notificationService.listByEngageChannel(securityContext.getUserPrincipal().getName(), engageChannelId);
    }
}
