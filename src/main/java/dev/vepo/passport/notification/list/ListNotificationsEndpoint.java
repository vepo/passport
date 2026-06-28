package dev.vepo.passport.notification.list;

import java.util.List;

import dev.vepo.passport.notification.NotificationService;
import dev.vepo.passport.notification.NotificationSummaryResponse;
import dev.vepo.passport.notification.UnreadCountResponse;
import io.quarkus.security.Authenticated;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.SecurityContext;

@ApplicationScoped
@Path("/notifications")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ListNotificationsEndpoint {

    private final NotificationService notificationService;

    @Inject
    public ListNotificationsEndpoint(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GET
    public List<NotificationSummaryResponse> list(@Context SecurityContext securityContext,
                                                  @QueryParam("unread") @DefaultValue("false") boolean unreadOnly) {
        return notificationService.listForUser(securityContext.getUserPrincipal().getName(), unreadOnly ? true : null);
    }

    @GET
    @Path("/unread-count")
    public UnreadCountResponse unreadCount(@Context SecurityContext securityContext) {
        return new UnreadCountResponse(notificationService.countUnreadForUser(securityContext.getUserPrincipal().getName()));
    }
}
