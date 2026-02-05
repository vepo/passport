package dev.vepo.passport.auth;

import jakarta.validation.constraints.NotEmpty;

public record ResetPasswordRequest(@NotEmpty String token,
                                   @NotEmpty String recoveryPassword,
                                   @NotEmpty String newPassword) {}
