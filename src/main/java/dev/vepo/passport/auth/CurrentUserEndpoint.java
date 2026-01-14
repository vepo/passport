package dev.vepo.passport.auth;

import dev.vepo.passport.user.Role;
import dev.vepo.passport.user.UserRepository;
import jakarta.annotation.security.RolesAllowed;
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
@RolesAllowed({ Role.USER_ROLE, Role.ADMIN_ROLE, Role.PROJECT_MANAGER_ROLE })
public class CurrentUserEndpoint {
    private final UserRepository userRepository;

    @Inject
    public CurrentUserEndpoint(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GET
    public AuthResponse requestAuthenticatedUserInformation(@Context SecurityContext ctx) {
        return userRepository.findByUsername(ctx.getUserPrincipal().getName())
                             .map(AuthResponse::load)
                             .orElseThrow(() -> new NotAuthorizedException("Unauthorized"));
    }
}
