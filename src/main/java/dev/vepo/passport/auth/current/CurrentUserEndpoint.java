package dev.vepo.passport.auth.current;

import dev.vepo.passport.user.UserRepository;
import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.SecurityContext;

@Path("/auth/me")
@Produces(MediaType.APPLICATION_JSON)
@Authenticated
public class CurrentUserEndpoint {
    private final UserRepository userRepository;

    @Inject
    public CurrentUserEndpoint(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GET
    public CurrentUserResponse requestAuthenticatedUserInformation(@Context SecurityContext ctx) {
        return userRepository.findByUsername(ctx.getUserPrincipal().getName())
                             .map(CurrentUserResponse::load)
                             .orElseThrow(() -> new NotAuthorizedException("Unauthorized"));
    }
}
