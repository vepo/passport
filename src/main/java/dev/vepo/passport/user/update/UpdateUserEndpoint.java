package dev.vepo.passport.user.update;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.vepo.passport.model.Profile;
import dev.vepo.passport.model.User;
import dev.vepo.passport.profile.ProfileRepository;
import dev.vepo.passport.shared.security.RequiredRoles;
import dev.vepo.passport.user.UserRepository;
import dev.vepo.passport.user.UserResponse;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response.Status;

@Path("/users/{userId}")
@ApplicationScoped
@RolesAllowed(RequiredRoles.ADMIN)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class UpdateUserEndpoint {
    private static final Logger logger = LoggerFactory.getLogger(UpdateUserEndpoint.class);

    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;

    @Inject
    public UpdateUserEndpoint(UserRepository userRepository, ProfileRepository profileRepository) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
    }

    @PUT
    @Transactional
    public UserResponse update(@PathParam("userId") long userId, @Valid UpdateUserRequest request) {
        logger.info("Updating user ID: {}", userId);

        User user = userRepository.findById(userId)
                                  .orElseThrow(() -> new NotFoundException("User not found! userId=%d".formatted(userId)));

        logger.info("Found user: {} (current email: {})", user.getUsername(), user.getEmail());

        // Check for email conflicts (if email is being changed)
        if (!user.getEmail().equalsIgnoreCase(request.email())) {
            checkEmailConflict(request.email(), userId);
        }

        // Update user fields
        user.setName(request.name());
        user.setEmail(request.email());

        // Update profiles if provided
        if (request.profileIds() != null) {
            Set<Profile> profiles = loadProfiles(request.profileIds());
            user.setProfiles(profiles);
            logger.info("Updated user profiles to: {}",
                        profiles.stream().map(Profile::getName).collect(Collectors.toList()));
        }

        // Save the updated user
        User savedUser = userRepository.save(user);
        logger.info("User updated successfully: {}", savedUser.getUsername());

        return UserResponse.load(savedUser);
    }

    private void checkEmailConflict(String newEmail, Long currentUserId) {
        Optional<User> existingUser = userRepository.findByEmail(newEmail);
        if (existingUser.isPresent() && !existingUser.get().getId().equals(currentUserId)) {
            throw new WebApplicationException("Email '%s' is already registered".formatted(newEmail), Status.CONFLICT);
        }
    }

    private Set<Profile> loadProfiles(Set<Long> profileIds) {
        if (profileIds == null || profileIds.isEmpty()) {
            return new HashSet<>();
        }

        var profiles = profileRepository.findByIds(profileIds);

        // Check if all requested profiles exist
        if (profiles.size() != profileIds.size()) {
            var foundIds = profiles.stream()
                                   .map(Profile::getId)
                                   .collect(Collectors.toSet());

            var missingIds = profileIds.stream()
                                       .filter(id -> !foundIds.contains(id))
                                       .collect(Collectors.toSet());

            throw new BadRequestException("One or more profiles not found: %s".formatted(missingIds));
        }

        return profiles;
    }
}