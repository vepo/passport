package dev.vepo.passport.profile.update;

import dev.vepo.passport.profile.ProfileRepository;
import dev.vepo.passport.profile.ProfileResponse;
import dev.vepo.passport.role.RoleRepository;
import dev.vepo.passport.shared.security.RequiredRoles;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/profiles/{profileId}")
@ApplicationScoped
@RolesAllowed(RequiredRoles.ADMIN)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class UpdateProfileEndpoint {

    private final ProfileRepository profileRepository;
    private final RoleRepository roleRepository;

    @Inject
    public UpdateProfileEndpoint(ProfileRepository profileRepository,
                                 RoleRepository roleRepository) {
        this.profileRepository = profileRepository;
        this.roleRepository = roleRepository;
    }

    @PUT
    @Transactional
    public ProfileResponse update(@PathParam("profileId") long profileId, UpdateProfileRequest request) {
        var profile = this.profileRepository.findById(profileId)
                                            .orElseThrow(() -> new NotFoundException("Profile not found! profileId=%d".formatted(profileId)));
        profile.setName(request.name());
        profile.setRoles(roleRepository.findByIds(request.roleIds()));
        return ProfileResponse.from(profile);
    }
}
