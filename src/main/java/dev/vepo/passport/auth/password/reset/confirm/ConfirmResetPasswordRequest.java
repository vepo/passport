package dev.vepo.passport.auth.password.reset.confirm;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public record ConfirmResetPasswordRequest(@NotEmpty String token,
                                          @NotEmpty String recoveryPassword,
                                          @NotEmpty @Size(min = 8, max = 20) String newPassword) {}
