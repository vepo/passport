package dev.vepo.passport.user;

import java.util.stream.Collectors;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.resteasy.reactive.ResponseStatus;

import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("users")
@ApplicationScoped
@RolesAllowed(Role.ADMIN_ROLE)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CreateUserEndpoint {
    private final UserRepository userRepository;
    private final String passwordDefault;

    @Inject
    public CreateUserEndpoint(UserRepository userRepository,
                              @ConfigProperty(name = "password.default") String passwordDefault) {
        this.userRepository = userRepository;
        this.passwordDefault = passwordDefault;
    }

    @POST
    @Transactional
    @ResponseStatus(201)
    @RolesAllowed(Role.ADMIN_ROLE)
    public UserResponse create(@Valid CreateUserRequest request) {
        return UserResponse.load(this.userRepository.save(new User(request.username(),
                                                                   request.name(),
                                                                   request.email(),
                                                                   passwordDefault,
                                                                   request.roles()
                                                                          .stream()
                                                                          .map(role -> Role.from(role)
                                                                                           .orElseThrow(() -> new BadRequestException("Role does not exists! role=%s".formatted(role))))
                                                                          .collect(Collectors.toSet()))));
    }
}
