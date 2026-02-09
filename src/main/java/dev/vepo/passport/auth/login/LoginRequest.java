package dev.vepo.passport.auth.login;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(@NotBlank(message = "Email must not be empty!") @Email(message = "It should be a valid email!") String email,
                           @NotBlank(message = "Password must not be empty!") String password) {}