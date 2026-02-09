package dev.vepo.passport.role.list;

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
import jakarta.ws.rs.core.MediaType;

@Path("/roles")
@ApplicationScoped
@RolesAllowed(RequiredRoles.ADMIN)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ListRolesEndpoint {
    private final RoleRepository roleRepository;

    @Inject
    public ListRolesEndpoint(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @GET
    public List<RoleResponse> list() {
        // Check for existing role with same name
        return this.roleRepository.findAll()
                                  .stream()
                                  .map(RoleResponse::from)
                                  .toList();
    }
}
