package dev.vepo.passport.mailer;

import java.time.Instant;

public record ResetPasswordCreatedEvent(String name, String username, String email, Instant requestedAt,
                                        Instant expireAt, String password, String token) {}
