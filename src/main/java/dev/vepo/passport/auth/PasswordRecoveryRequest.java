package dev.vepo.passport.auth;

import jakarta.validation.constraints.Email;

public record PasswordRecoveryRequest(@Email String email) {}
