package dev.vepo.passport.auth;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.stream.Collectors;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import dev.vepo.passport.user.Role;
import dev.vepo.passport.user.UserRepository;
import io.smallrye.jwt.build.Jwt;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.SecurityContext;

@Path("/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthenticationEndpoint {
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final String issuer;

    @Inject
    public AuthenticationEndpoint(PasswordEncoder passwordEncoder,
                                  UserRepository userRepository,
                                  @ConfigProperty(name = "mp.jwt.verify.issuer") String issuer) {
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
        this.issuer = issuer;
    }

    @POST
    @Path("/login")
    public LoginResponse login(@Valid LoginRequest request) {
        return this.userRepository.findByEmail(request.email())
                                  .filter(u -> passwordEncoder.matches(request.password(), u.getEncodedPassword()))
                                  .map(user -> {
                                      Instant now = Instant.now();
                                      return new LoginResponse(Jwt.issuer(issuer)
                                                                  .upn(user.getUsername())
                                                                  .claim("username", user.getUsername())
                                                                  .claim("id", user.getId())
                                                                  .claim("email", user.getEmail())
                                                                  .groups(user.getRoles().stream()
                                                                              .map(Role::role)
                                                                              .collect(Collectors.toSet()))
                                                                  .issuedAt(now)
                                                                  .expiresAt(now.plus(1, ChronoUnit.DAYS))
                                                                  .sign());
                                  })
                                  .orElseThrow(() -> new NotAuthorizedException("Invalid credentials!", request));
    }

    @GET
    @Path("/me")
    @RolesAllowed({ Role.USER_ROLE, Role.ADMIN_ROLE, Role.PROJECT_MANAGER_ROLE })
    public AuthResponse me(@Context SecurityContext ctx) {
        return userRepository.findByUsername(ctx.getUserPrincipal().getName())
                             .map(AuthResponse::load)
                             .orElseThrow(() -> new NotFoundException("User not found!"));
    }
}
