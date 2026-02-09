package dev.vepo.passport.user.create;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jboss.resteasy.reactive.ResponseStatus;

import dev.vepo.passport.mailer.UserCreatedEvent;
import dev.vepo.passport.model.Profile;
import dev.vepo.passport.model.User;
import dev.vepo.passport.profile.ProfileRepository;
import dev.vepo.passport.shared.security.PasswordEncoder;
import dev.vepo.passport.shared.security.PasswordGenerator;
import dev.vepo.passport.shared.security.RequiredRoles;
import dev.vepo.passport.user.UserRepository;
import dev.vepo.passport.user.UserResponse;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response.Status;

@Path("/users")
@ApplicationScoped
@RolesAllowed(RequiredRoles.ADMIN)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CreateUserEndpoint {
    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordGenerator passwordGenerator;
    private final Event<UserCreatedEvent> userCreatedEmmiter;

    @Inject
    public CreateUserEndpoint(UserRepository userRepository,
                              ProfileRepository profileRepository,
                              PasswordEncoder passwordEncoder,
                              PasswordGenerator passwordGenerator,
                              Event<UserCreatedEvent> userCreatedEmmiter) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordGenerator = passwordGenerator;
        this.userCreatedEmmiter = userCreatedEmmiter;
    }

    @POST
    @Transactional
    @ResponseStatus(201)
    public UserResponse create(@Valid CreateUserRequest request) {
        String password = passwordGenerator.generate();
        var conflicts = this.userRepository.findByUsernameOrEmail(request.username(), request.email());
        if (!conflicts.isEmpty()) {
            handleConflicts(conflicts, request);
        }
        var user = this.userRepository.save(new User(request.username(),
                                                     request.name(),
                                                     request.email(),
                                                     passwordEncoder.hashPassword(password),
                                                     loadProfiles(request.profileIds())));
        userCreatedEmmiter.fireAsync(new UserCreatedEvent(user.getId(),
                                                          user.getName(),
                                                          user.getUsername(),
                                                          user.getEmail(),
                                                          user.getCreatedAt(),
                                                          password));
        return UserResponse.load(user);
    }

    private void handleConflicts(List<User> conflicts, CreateUserRequest request) {
        var conflictMessages = new HashSet<String>();

        for (User existingUser : conflicts) {
            if (existingUser.getUsername().equalsIgnoreCase(request.username())) {
                conflictMessages.add("Username '%s' already exists".formatted(request.username()));
            }

            if (existingUser.getEmail().equalsIgnoreCase(request.email())) {
                conflictMessages.add("Email '%s' is already registered".formatted(request.email()));
            }
        }

        throw new WebApplicationException(String.join("; ", conflictMessages), Status.CONFLICT);
    }

    private Set<Profile> loadProfiles(Set<Long> profileIds) {
        var profiles = profileRepository.findByIds(profileIds);
        if (profiles.size() != profileIds.size()) {
            throwProfileNotFoundException(profileIds, profiles);
        }
        return profiles;
    }

    private void throwProfileNotFoundException(Set<Long> profileIds, Set<Profile> profiles) {
        var foundProfiles = profiles.stream().map(Profile::getId).toList();
        var notFoundProfiles = profileIds.stream().filter(id -> !foundProfiles.contains(id)).toList();
        throw new NotFoundException("Could not find profiles! ids=%s".formatted(notFoundProfiles));
    }
}
