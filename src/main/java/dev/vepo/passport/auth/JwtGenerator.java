package dev.vepo.passport.auth;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.stream.Collectors;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import dev.vepo.passport.model.Role;
import dev.vepo.passport.model.User;
import io.smallrye.jwt.build.Jwt;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class JwtGenerator {
    private final String issuer;

    public JwtGenerator(@ConfigProperty(name = "mp.jwt.verify.issuer") String issuer) {
        this.issuer = issuer;
    }

    public String generate(User user) {
        Instant now = Instant.now();
        return Jwt.issuer(issuer)
                  .upn(user.getUsername())
                  .claim("username", user.getUsername())
                  .claim("id", user.getId())
                  .claim("email", user.getEmail())
                  .groups(user.getProfiles()
                              .stream()
                              .flatMap(profile -> profile.getRoles().stream())
                              .distinct()
                              .map(Role::getName)
                              .collect(Collectors.toSet()))
                  .issuedAt(now)
                  .expiresAt(now.plus(1, ChronoUnit.DAYS))
                  .sign();
    }
}
