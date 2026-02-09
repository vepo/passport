package dev.vepo.passport.role.create;

import org.jboss.resteasy.reactive.ResponseStatus;

import dev.vepo.passport.model.Role;
import dev.vepo.passport.role.RoleRepository;
import dev.vepo.passport.role.RoleResponse;
import dev.vepo.passport.shared.security.RequiredRoles;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response.Status;

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
    @Transactional
    @ResponseStatus(201)
    public RoleResponse create(@Valid CreateRoleRequest request) {
        // Check for existing role with same name
        roleRepository.findByName(request.name())
                      .ifPresent(existingRole -> {
                          throw new WebApplicationException("Role with name '%s' already exists".formatted(request.name()),
                                                            Status.CONFLICT);
                      });

        Role role = roleRepository.save(new Role(request.name()));
        return RoleResponse.from(role);
    }
}