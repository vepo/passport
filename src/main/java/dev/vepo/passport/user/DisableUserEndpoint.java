package dev.vepo.passport.user;

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

@Path("users/{userId}/disable")
@ApplicationScoped
@RolesAllowed(Role.ADMIN_ROLE)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class DisableUserEndpoint {
    private final UserRepository userRepository;

    @Inject
    public DisableUserEndpoint(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @POST
    @Transactional
    public UserResponse update(@PathParam("userId") long userId) {
        return UserResponse.load(this.userRepository.findById(userId)
                                                    .map(user -> {
                                                        user.setDeleted(true);
                                                        this.userRepository.save(user);
                                                        return user;
                                                    })
                                                    .orElseThrow(() -> new NotFoundException("User not found!!! userId=%d".formatted(userId))));
    }
}
