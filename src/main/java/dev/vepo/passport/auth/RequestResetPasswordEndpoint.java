package dev.vepo.passport.auth;

import dev.vepo.passport.user.Role;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/api/auth/recovery")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed({ Role.USER_ROLE, Role.ADMIN_ROLE, Role.PROJECT_MANAGER_ROLE })
public class RequestResetPasswordEndpoint {

}
