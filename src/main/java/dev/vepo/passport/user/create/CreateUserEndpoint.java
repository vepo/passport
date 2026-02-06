package dev.vepo.passport.user.create;

import dev.vepo.passport.mailer.UserCreatedEvent;
import dev.vepo.passport.shared.security.PasswordEncoder;
import dev.vepo.passport.shared.security.PasswordGenerator;
import dev.vepo.passport.shared.security.RequiredRoles;
import dev.vepo.passport.user.UserRepository;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/users")
@ApplicationScoped
@RolesAllowed(RequiredRoles.ADMIN)
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

    // @POST
    // @Transactional
    // @ResponseStatus(201)
    // public UserResponse create(@Valid CreateUserRequest request) {
    // String password = passwordGenerator.generate();
    // var user = this.userRepository.save(new User(request.username(),
    // request.name(),
    // request.email(),
    // passwordEncoder.hashPassword(password),
    // request.roles()
    // .stream()
    // .map(role -> Role.from(role)
    // .orElseThrow(() -> new BadRequestException("Role does not exists!
    // role=%s".formatted(role))))
    // .collect(Collectors.toSet())));
    // userCreatedEmmiter.fireAsync(new UserCreatedEvent(user.getId(),
    // user.getName(),
    // user.getUsername(),
    // user.getEmail(),
    // user.getCreatedAt(),
    // password));
    // return UserResponse.load(user);
    // }
}
