package dev.vepo.passport.auth.password.change;

import dev.vepo.passport.shared.security.PasswordEncoder;
import dev.vepo.passport.user.UserRepository;
import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.SecurityContext;

@Authenticated
@Path("auth/change-password")
@Produces(MediaType.APPLICATION_JSON)
public class ChangePasswordEndpoint {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Inject
    public ChangePasswordEndpoint(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @POST
    @Transactional
    public Response change(@Context SecurityContext ctx, @Valid ChangePasswordRequest request) {
        return userRepository.findActiveByUsernameAndPassword(ctx.getUserPrincipal().getName(),
                                                              passwordEncoder.hashPassword(request.currentPassword()))
                             .map(user -> {
                                 user.setEncodedPassword(passwordEncoder.hashPassword(request.newPassword()));
                                 userRepository.save(user);
                                 return Response.ok()
                                                .build();
                             })
                             .orElseGet(() -> Response.status(Status.FORBIDDEN)
                                                      .build());
    }
}
