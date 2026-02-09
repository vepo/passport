package dev.vepo.passport.user.assignprofiles;

import java.util.Set;

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
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/users/{userId}/profiles")
@ApplicationScoped
@RolesAllowed(RequiredRoles.ADMIN)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AssignProfilesEndpoint {
    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;

    @Inject
    public AssignProfilesEndpoint(UserRepository userRepository,
                                  ProfileRepository profileRepository) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
    }

    @POST
    @Transactional
    public UserResponse assignProfiles(@PathParam("userId") Long userId,
                                       @Valid AssignProfilesRequest request) {
        // Find the user
        User user = userRepository.findById(userId)
                                  .orElseThrow(() -> new NotFoundException("User not found with id: %d".formatted(userId)));

        // Check if user is disabled
        if (user.isDisabled()) {
            throw new NotFoundException("Cannot assign profiles to disabled user");
        }

        // Load all requested profiles
        Set<Profile> profiles = profileRepository.findByIds(request.profileIds());

        // Verify all requested profiles exist
        if (profiles.size() != request.profileIds().size()) {
            throwProfileNotFoundException(request.profileIds(), profiles);
        }

        // Update user profiles
        user.setProfiles(profiles);

        // Save the updated user
        return UserResponse.load(userRepository.save(user));
    }

    private void throwProfileNotFoundException(Set<Long> requestedProfileIds, Set<Profile> foundProfiles) {
        var foundProfileIds = foundProfiles.stream()
                                           .map(Profile::getId)
                                           .toList();
        var notFoundProfileIds = requestedProfileIds.stream()
                                                    .filter(id -> !foundProfileIds.contains(id))
                                                    .toList();
        throw new NotFoundException("Could not find profiles! ids=%s".formatted(notFoundProfileIds));
    }
}