package dev.vepo.passport.profile.enable;

import dev.vepo.passport.profile.ProfileRepository;
import dev.vepo.passport.profile.ProfileResponse;
import dev.vepo.passport.shared.security.RequiredRoles;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/profiles/{profileId}/enable")
@ApplicationScoped
@RolesAllowed(RequiredRoles.ADMIN)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class EnableProfileEndpoint {
    private final ProfileRepository profileRepository;

    @Inject
    public EnableProfileEndpoint(ProfileRepository profileRepository) {
        this.profileRepository = profileRepository;
    }

    @POST
    @Transactional
    public ProfileResponse enable(@PathParam("profileId") long profileId) {
        var profile = this.profileRepository.findById(profileId)
                                            .orElseThrow(() -> new NotFoundException("Profile not found! profileId=%d".formatted(profileId)));
        profile.setDisabled(false);
        return ProfileResponse.from(this.profileRepository.save(profile));
    }
}