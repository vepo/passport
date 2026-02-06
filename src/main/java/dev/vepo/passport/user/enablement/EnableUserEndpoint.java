package dev.vepo.passport.user.enablement;

import dev.vepo.passport.shared.security.RequiredRoles;
import dev.vepo.passport.user.UserRepository;
import dev.vepo.passport.user.UserResponse;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/users/{userId}/enable")
@ApplicationScoped
@RolesAllowed(RequiredRoles.ADMIN)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class EnableUserEndpoint {
    private final UserRepository userRepository;

    @Inject
    public EnableUserEndpoint(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @POST
    @Transactional
    public UserResponse update(@PathParam("userId") long userId) {
        return UserResponse.load(this.userRepository.findById(userId)
                                                    .map(user -> {
                                                        user.setDeleted(false);
                                                        this.userRepository.save(user);
                                                        return user;
                                                    })
                                                    .orElseThrow(() -> new NotFoundException("User not found!!! userId=%d".formatted(userId))));
    }
}
