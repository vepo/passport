package dev.vepo.passport.auth.password.reset.request;

import jakarta.validation.constraints.Email;

public record RequestResetPasswordRequest(@Email String email) {}
