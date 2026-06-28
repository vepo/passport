package dev.vepo.passport.notification.create;

import dev.vepo.passport.notification.CreateInternalNotificationRequest;
import dev.vepo.passport.notification.NotificationResponse;
import dev.vepo.passport.notification.NotificationService;
import jakarta.annotation.security.PermitAll;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@ApplicationScoped
@Path("/internal/notifications")
@PermitAll
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CreateInternalNotificationEndpoint {

    private final NotificationService notificationService;

    @Inject
    public CreateInternalNotificationEndpoint(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @POST
    public Response create(@Valid CreateInternalNotificationRequest request) {
        NotificationResponse response = notificationService.publishInternalNotification(request);
        return Response.status(Response.Status.CREATED).entity(response).build();
    }
}
