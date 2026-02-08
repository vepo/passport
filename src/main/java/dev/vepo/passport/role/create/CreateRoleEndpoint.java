package dev.vepo.passport.role.create;

import dev.vepo.passport.model.Role;
import dev.vepo.passport.role.RoleRepository;
import dev.vepo.passport.shared.security.RequiredRoles;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/roles")
@ApplicationScoped
@RolesAllowed(RequiredRoles.ADMIN)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CreateRoleEndpoint {
    private final RoleRepository roleRepository;

    @Inject
    public CreateRoleEndpoint(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @POST
    public Response create(@Valid CreateRoleRequest request) {
        roleRepository.save(new Role(request.name()));
        return Response.ok().build();
    }
}
