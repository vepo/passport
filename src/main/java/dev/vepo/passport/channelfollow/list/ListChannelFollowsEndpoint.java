package dev.vepo.passport.channelfollow.list;

import java.util.List;

import dev.vepo.passport.channelfollow.ChannelFollowResponse;
import dev.vepo.passport.channelfollow.ChannelFollowService;
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
@Path("/channel-follows")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ListChannelFollowsEndpoint {

    private final ChannelFollowService channelFollowService;

    @Inject
    public ListChannelFollowsEndpoint(ChannelFollowService channelFollowService) {
        this.channelFollowService = channelFollowService;
    }

    @GET
    public List<ChannelFollowResponse> list(@Context SecurityContext securityContext) {
        return channelFollowService.listForUser(securityContext.getUserPrincipal().getName());
    }
}
