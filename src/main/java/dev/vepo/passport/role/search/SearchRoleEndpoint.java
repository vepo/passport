package dev.vepo.passport.role.search;

import java.util.List;

import dev.vepo.passport.role.RoleRepository;
import dev.vepo.passport.role.RoleResponse;
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

@Path("/roles/search")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed(RequiredRoles.ADMIN)
public class SearchRoleEndpoint {
    private final RoleRepository roleRepository;

    @Inject
    public SearchRoleEndpoint(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @GET
    public List<RoleResponse> search(@QueryParam("name") String name) {
        return roleRepository.search()
                             .name(name)
                             .execute()
                             .stream()
                             .map(RoleResponse::from)
                             .toList();
    }
}
