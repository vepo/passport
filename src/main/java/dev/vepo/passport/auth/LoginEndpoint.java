package dev.vepo.passport.auth;

import dev.vepo.passport.user.UserRepository;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/auth/login")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class LoginEndpoint {
    private final PasswordEncoder passwordEncoder;
    private final JwtGenerator jwtGenerator;
    private final UserRepository userRepository;

    @Inject
    public LoginEndpoint(PasswordEncoder passwordEncoder,
                         JwtGenerator jwtGenerator,
                         UserRepository userRepository) {
        this.jwtGenerator = jwtGenerator;
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
    }

    @POST
    public LoginResponse login(@Valid LoginRequest request) {
        return this.userRepository.findByEmail(request.email())
                                  .filter(u -> passwordEncoder.matches(request.password(), u.getEncodedPassword()))
                                  .map(user -> new LoginResponse(jwtGenerator.generate(user)))
                                  .orElseThrow(() -> new NotAuthorizedException("Invalid credentials!", request));
    }

}
