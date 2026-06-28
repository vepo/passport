package dev.vepo.passport.channelfollow.delete;

import dev.vepo.passport.channelfollow.ChannelFollowService;
import io.quarkus.security.Authenticated;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

@ApplicationScoped
@Path("/channel-follows/{engageChannelId}")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class DeleteChannelFollowEndpoint {

    private final ChannelFollowService channelFollowService;

    @Inject
    public DeleteChannelFollowEndpoint(ChannelFollowService channelFollowService) {
        this.channelFollowService = channelFollowService;
    }

    @DELETE
    public Response unfollow(@Context SecurityContext securityContext, @PathParam("engageChannelId") Long engageChannelId) {
        channelFollowService.unfollow(securityContext.getUserPrincipal().getName(), engageChannelId);
        return Response.noContent().build();
    }
}
