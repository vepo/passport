package dev.vepo.passport.profile.list;

import java.util.List;

import dev.vepo.passport.profile.ProfileRepository;
import dev.vepo.passport.profile.ProfileResponse;
import dev.vepo.passport.shared.security.RequiredRoles;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/profiles")
@ApplicationScoped
@RolesAllowed(RequiredRoles.ADMIN)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ListProfilesEndpoint {
    private final ProfileRepository profileRepository;

    @Inject
    public ListProfilesEndpoint(ProfileRepository profileRepository) {
        this.profileRepository = profileRepository;
    }

    @GET
    public List<ProfileResponse> list() {
        // Check for existing role with same name
        return this.profileRepository.findAll()
                                     .stream()
                                     .map(ProfileResponse::from)
                                     .toList();
    }
}
