package dev.vepo.passport.channelfollow.find;

import dev.vepo.passport.channelfollow.ChannelFollowService;
import dev.vepo.passport.channelfollow.ChannelFollowStatusResponse;
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
@Path("/channel-follows/{engageChannelId}/status")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ChannelFollowStatusEndpoint {

    private final ChannelFollowService channelFollowService;

    @Inject
    public ChannelFollowStatusEndpoint(ChannelFollowService channelFollowService) {
        this.channelFollowService = channelFollowService;
    }

    @GET
    public ChannelFollowStatusResponse status(@Context SecurityContext securityContext,
                                              @PathParam("engageChannelId") Long engageChannelId) {
        return new ChannelFollowStatusResponse(channelFollowService.isFollowing(securityContext.getUserPrincipal().getName(),
                                                                                engageChannelId));
    }
}
