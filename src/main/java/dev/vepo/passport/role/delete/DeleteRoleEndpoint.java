package dev.vepo.passport.role.delete;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.vepo.passport.role.RoleRepository;
import dev.vepo.passport.role.RoleResponse;
import dev.vepo.passport.shared.security.RequiredRoles;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/roles/{roleId}")
@ApplicationScoped
@RolesAllowed(RequiredRoles.ADMIN)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class DeleteRoleEndpoint {
    private static final Logger logger = LoggerFactory.getLogger(DeleteRoleEndpoint.class);
    private final RoleRepository roleRepository;

    @Inject
    public DeleteRoleEndpoint(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @DELETE
    @Transactional
    public RoleResponse delete(@PathParam("roleId") long roleId) {
        var role = this.roleRepository.findById(roleId)
                                      .orElseThrow(() -> new NotFoundException("Role not found! roleId=%d".formatted(roleId)));
        logger.info("Deleting role: {}", role);
        roleRepository.delete(role.getId());
        return RoleResponse.from(role);
    }
}
