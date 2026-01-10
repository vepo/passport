package dev.vepo.passport.user;

import java.util.List;

import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

@Path("users/search")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed({ Role.ADMIN_ROLE, Role.PROJECT_MANAGER_ROLE, Role.USER_ROLE })
public class SearchUserEndpoint {

    private final UserRepository userRepository;

    @Inject
    public SearchUserEndpoint(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GET
    public List<UserResponse> search(@QueryParam("name") String name,
                                     @QueryParam("email") String email,
                                     @QueryParam("roles") List<String> roles) {
        return userRepository.search(name, email, roles.stream().map(role -> Role.from(role)
                                                                                 .orElseThrow(() -> new BadRequestException("Role does not exist! role=%s".formatted(role))))
                                                       .toList())
                             .map(UserResponse::load)
                             .toList();
    }
}
