package dev.vepo.passport.auth;

import dev.vepo.passport.user.UserRepository;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

@Path("/auth/reset")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ResetPasswordEndpoint {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Inject
    public ResetPasswordEndpoint(UserRepository userRepository,
                                 PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @POST
    @Transactional
    public Response reset(@Valid ResetPasswordRequest request) {
        return userRepository.findValidResetPasswordTokenByTokenAndPassword(request.token(), passwordEncoder.hashPassword(request.recoveryPassword()))
                             .map(token -> {
                                 token.setUsed(true);
                                 token.getUser().setEncodedPassword(passwordEncoder.hashPassword(request.newPassword()));
                                 userRepository.save(token);
                                 return Response.ok().build();
                             })
                             .orElseGet(() -> Response.status(Status.NOT_FOUND).build());
    }
}
