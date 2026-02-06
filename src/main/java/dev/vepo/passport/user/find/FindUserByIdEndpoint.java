package dev.vepo.passport.user.find;

import dev.vepo.passport.shared.security.RequiredRoles;
import dev.vepo.passport.user.UserRepository;
import dev.vepo.passport.user.UserResponse;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/users/{userId}")
@ApplicationScoped
@RolesAllowed(RequiredRoles.ADMIN)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class FindUserByIdEndpoint {
    private final UserRepository userRepository;

    @Inject
    public FindUserByIdEndpoint(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GET
    public UserResponse findUserById(@PathParam("userId") long userId) {
        return userRepository.findById(userId)
                             .map(UserResponse::load)
                             .orElseThrow(() -> new NotFoundException("User not found!!! userId=%s".formatted(userId)));
    }
}
