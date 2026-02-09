package dev.vepo.passport.auth.password.reset.request;

import java.time.Duration;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.vepo.passport.mailer.ResetPasswordCreatedEvent;
import dev.vepo.passport.model.ResetPasswordToken;
import dev.vepo.passport.model.User;
import dev.vepo.passport.shared.security.PasswordEncoder;
import dev.vepo.passport.shared.security.PasswordGenerator;
import dev.vepo.passport.user.UserRepository;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/auth/request-reset-password")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RequestResetPasswordEndpoint {

    private static final Logger logger = LoggerFactory.getLogger(RequestResetPasswordEndpoint.class);
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordGenerator passwordGenerator;
    private final Event<ResetPasswordCreatedEvent> resetPasswordCreatedEmmiter;

    @Inject
    public RequestResetPasswordEndpoint(UserRepository userRepository,
                                        PasswordEncoder passwordEncoder,
                                        PasswordGenerator passwordGenerator,
                                        Event<ResetPasswordCreatedEvent> resetPasswordCreatedEmmiter) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordGenerator = passwordGenerator;
        this.resetPasswordCreatedEmmiter = resetPasswordCreatedEmmiter;
    }

    @POST
    @Transactional
    public Response recovery(@Valid RequestResetPasswordRequest request) {
        userRepository.findByEmail(request.email())
                      .ifPresentOrElse(this::recovery, () -> logger.warn("No user found! Ignoring... request={}", request));
        return Response.ok()
                       .build();
    }

    private void recovery(User user) {
        this.userRepository.findValidResetPasswordTokenByUserId(user.getId())
                           .ifPresentOrElse(toke -> logger.warn("There is one reset password token active for user! username={}", user.getUsername()),
                                            () -> createPasswordToken(user));
    }

    private void createPasswordToken(User user) {
        var tokenPassword = passwordGenerator.generate();
        var token = new ResetPasswordToken(UUID.randomUUID().toString(),
                                           passwordEncoder.hashPassword(tokenPassword),
                                           user);
        this.userRepository.save(token);
        resetPasswordCreatedEmmiter.fireAsync(new ResetPasswordCreatedEvent(user.getName(),
                                                                            user.getUsername(),
                                                                            user.getEmail(),
                                                                            token.getRequestedAt(),
                                                                            token.getRequestedAt()
                                                                                 .plus(Duration.ofDays(1)),
                                                                            tokenPassword,
                                                                            token.getToken()));
    }
}
