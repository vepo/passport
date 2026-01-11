package dev.vepo.passport.user;

import java.util.stream.Collectors;

import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("users/{userId}")
@ApplicationScoped
@RolesAllowed(Role.ADMIN_ROLE)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class UpdateUserEndpoint {

    private final UserRepository userRepository;

    @Inject
    public UpdateUserEndpoint(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @POST
    @Transactional
    public UserResponse update(@PathParam("userId") long userId, @Valid CreateUserRequest request) {
        return UserResponse.load(this.userRepository.findById(userId)
                                                    .map(user -> {
                                                        user.setEmail(request.email());
                                                        user.setName(request.name());
                                                        user.setRoles(request.roles()
                                                                             .stream()
                                                                             .map(role -> Role.from(role)
                                                                                              .orElseThrow(() -> new BadRequestException("Role does not exists! role=%s".formatted(role))))
                                                                             .collect(Collectors.toSet()));
                                                        this.userRepository.save(user);
                                                        return user;
                                                    })
                                                    .orElseThrow(() -> new NotFoundException("User not found!!! userId=%d".formatted(userId))));
    }
}
