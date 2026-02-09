package dev.vepo.passport.profile.find;

import dev.vepo.passport.profile.ProfileRepository;
import dev.vepo.passport.profile.ProfileResponse;
import dev.vepo.passport.shared.security.RequiredRoles;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/profiles/{profileId}")
@ApplicationScoped
@RolesAllowed(RequiredRoles.ADMIN)
@Produces(MediaType.APPLICATION_JSON)
public class FindProfileByIdEndpoint {
    private final ProfileRepository profileRepository;

    @Inject
    public FindProfileByIdEndpoint(ProfileRepository profileRepository) {
        this.profileRepository = profileRepository;
    }

    @GET
    public ProfileResponse findById(@PathParam("profileId") Long profileId) {
        return profileRepository.findById(profileId)
                                .map(ProfileResponse::from)
                                .orElseThrow(() -> new NotFoundException("Profile not found with id: %d".formatted(profileId)));
    }
}