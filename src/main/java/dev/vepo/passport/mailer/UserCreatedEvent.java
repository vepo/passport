package dev.vepo.passport.mailer;

import java.time.Instant;

public record UserCreatedEvent(long id, String name, String username,
                               String email, Instant createdAt, String password) {

}
