package dev.vepo.passport.profile.assignroles;

import java.util.Set;

import dev.vepo.passport.model.Profile;
import dev.vepo.passport.model.Role;
import dev.vepo.passport.profile.ProfileRepository;
import dev.vepo.passport.profile.ProfileResponse;
import dev.vepo.passport.role.RoleRepository;
import dev.vepo.passport.shared.security.RequiredRoles;
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

@Path("/profiles/{profileId}/roles")
@ApplicationScoped
@RolesAllowed(RequiredRoles.ADMIN)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AssignRolesEndpoint {
    private final ProfileRepository profileRepository;
    private final RoleRepository roleRepository;

    @Inject
    public AssignRolesEndpoint(ProfileRepository profileRepository,
                               RoleRepository roleRepository) {
        this.profileRepository = profileRepository;
        this.roleRepository = roleRepository;
    }

    @POST
    @Transactional
    public ProfileResponse assignRoles(@PathParam("profileId") Long profileId,
                                       @Valid AssignRolesRequest request) {
        // Find the profile
        Profile profile = profileRepository.findById(profileId)
                                           .orElseThrow(() -> new NotFoundException("Profile not found with id: %d".formatted(profileId)));

        // Load all requested roles
        Set<Role> roles = roleRepository.findByIds(request.roleIds());

        // Verify all requested roles exist
        if (roles.size() != request.roleIds().size()) {
            throwRoleNotFoundException(request.roleIds(), roles);
        }

        // Update profile roles
        profile.setRoles(roles);

        // Save the updated profile
        Profile updatedProfile = profileRepository.save(profile);

        return ProfileResponse.from(updatedProfile);
    }

    private void throwRoleNotFoundException(Set<Long> requestedRoleIds, Set<Role> foundRoles) {
        var foundRoleIds = foundRoles.stream()
                                     .map(Role::getId)
                                     .toList();
        var notFoundRoleIds = requestedRoleIds.stream()
                                              .filter(id -> !foundRoleIds.contains(id))
                                              .toList();
        throw new NotFoundException("Could not find roles! ids=%s".formatted(notFoundRoleIds));
    }
}