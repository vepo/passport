package dev.vepo.passport.auth.password.reset.confirm;

import jakarta.validation.constraints.NotEmpty;

public record ConfirmResetPasswordRequest(@NotEmpty String token,
                                          @NotEmpty String recoveryPassword,
                                          @NotEmpty String newPassword) {}
