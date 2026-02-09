package dev.vepo.passport.user.search;

import java.util.List;

import dev.vepo.passport.shared.security.RequiredRoles;
import dev.vepo.passport.user.UserRepository;
import dev.vepo.passport.user.UserResponse;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

@Path("/users/search")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed(RequiredRoles.ADMIN)
public class SearchUserEndpoint {

    private final UserRepository userRepository;

    @Inject
    public SearchUserEndpoint(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GET
    public List<UserResponse> search(@QueryParam("name") String name,
                                     @QueryParam("email") String email,
                                     @QueryParam("profiles") List<Long> profiles,
                                     @QueryParam("roles") List<Long> roles) {
        return userRepository.search()
                             .name(name)
                             .email(email)
                             .profileIds(profiles)
                             .roleIds(roles)
                             .execute()
                             .stream()
                             .map(UserResponse::load)
                             .toList();
    }
}
