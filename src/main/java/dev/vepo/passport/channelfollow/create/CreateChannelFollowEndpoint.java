package dev.vepo.passport.channelfollow.create;

import dev.vepo.passport.channelfollow.ChannelFollowResponse;
import dev.vepo.passport.channelfollow.ChannelFollowService;
import dev.vepo.passport.channelfollow.CreateChannelFollowRequest;
import io.quarkus.security.Authenticated;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

@ApplicationScoped
@Path("/channel-follows")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CreateChannelFollowEndpoint {

    private final ChannelFollowService channelFollowService;

    @Inject
    public CreateChannelFollowEndpoint(ChannelFollowService channelFollowService) {
        this.channelFollowService = channelFollowService;
    }

    @POST
    public Response follow(@Context SecurityContext securityContext, @Valid CreateChannelFollowRequest request) {
        ChannelFollowResponse response = channelFollowService.follow(securityContext.getUserPrincipal().getName(),
                                                                     request.engageChannelId());
        return Response.status(Response.Status.CREATED).entity(response).build();
    }
}
