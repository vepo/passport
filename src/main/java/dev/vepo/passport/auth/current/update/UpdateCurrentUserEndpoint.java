package dev.vepo.passport.auth.current.update;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.vepo.passport.auth.current.CurrentUserResponse;
import dev.vepo.passport.model.User;
import dev.vepo.passport.user.UserRepository;
import io.quarkus.security.Authenticated;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.SecurityContext;

@Path("/auth/me")
@ApplicationScoped
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class UpdateCurrentUserEndpoint {
    private static final Logger logger = LoggerFactory.getLogger(UpdateCurrentUserEndpoint.class);

    private final UserRepository userRepository;

    @Inject
    public UpdateCurrentUserEndpoint(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @PUT
    @Transactional
    public CurrentUserResponse update(@Context SecurityContext ctx, @Valid UpdateCurrentUserRequest request) {
        User user = userRepository.findActiveByUsername(ctx.getUserPrincipal().getName())
                                  .orElseThrow(() -> new NotAuthorizedException("Unauthorized"));

        if (!user.getEmail().equalsIgnoreCase(request.email())) {
            checkEmailConflict(request.email(), user.getId());
        }

        user.setName(request.name());
        user.setEmail(request.email());
        user.setDescription(request.description());
        User saved = userRepository.save(user);
        logger.info("Current user updated: {}", saved.getUsername());
        return CurrentUserResponse.load(saved);
    }

    private void checkEmailConflict(String newEmail, Long currentUserId) {
        Optional<User> existingUser = userRepository.findByEmail(newEmail);
        if (existingUser.isPresent() && !existingUser.get().getId().equals(currentUserId)) {
            throw new WebApplicationException("Email '%s' is already registered".formatted(newEmail), Status.CONFLICT);
        }
    }
}
