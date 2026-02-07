package dev.vepo.passport.auth.password.reset.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record RequestResetPasswordRequest(@Email @NotBlank String email) {}
