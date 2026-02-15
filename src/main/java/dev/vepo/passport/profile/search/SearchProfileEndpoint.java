package dev.vepo.passport.profile.search;

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
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

@Path("/profiles/search")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed(RequiredRoles.ADMIN)
public class SearchProfileEndpoint {
    private final ProfileRepository profileRepository;

    @Inject
    public SearchProfileEndpoint(ProfileRepository profileRepository) {
        this.profileRepository = profileRepository;
    }

    @GET
    public List<ProfileResponse> search(@QueryParam("name") String name,
                                        @QueryParam("roles") List<Long> roles,
                                        @QueryParam("disabled") Boolean disabled) {
        return profileRepository.search()
                                .name(name)
                                .roleIds(roles)
                                .disabled(disabled)
                                .execute()
                                .stream()
                                .map(ProfileResponse::from)
                                .toList();
    }
}
