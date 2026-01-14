package dev.vepo.passport.user;

import java.util.stream.Collectors;

import org.jboss.resteasy.reactive.ResponseStatus;

import dev.vepo.passport.auth.PasswordEncoder;
import dev.vepo.passport.mailer.UserCreatedEvent;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
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
    private final PasswordEncoder passwordEncoder;
    private final PasswordGenerator passwordGenerator;
    private final Event<UserCreatedEvent> userCreatedEmmiter;

    @Inject
    public CreateUserEndpoint(UserRepository userRepository,
                              PasswordEncoder passwordEncoder,
                              PasswordGenerator passwordGenerator,
                              Event<UserCreatedEvent> userCreatedEmmiter) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordGenerator = passwordGenerator;
        this.userCreatedEmmiter = userCreatedEmmiter;
    }

    @POST
    @Transactional
    @ResponseStatus(201)
    public UserResponse create(@Valid CreateUserRequest request) {
        String password = passwordGenerator.generate();
        var user = this.userRepository.save(new User(request.username(),
                                                     request.name(),
                                                     request.email(),
                                                     passwordEncoder.hashPassword(password),
                                                     request.roles()
                                                            .stream()
                                                            .map(role -> Role.from(role)
                                                                             .orElseThrow(() -> new BadRequestException("Role does not exists! role=%s".formatted(role))))
                                                            .collect(Collectors.toSet())));
        userCreatedEmmiter.fireAsync(new UserCreatedEvent(user.getId(),
                                                          user.getName(),
                                                          user.getUsername(),
                                                          user.getEmail(),
                                                          user.getCreatedAt(),
                                                          password));
        return UserResponse.load(user);
    }
}
