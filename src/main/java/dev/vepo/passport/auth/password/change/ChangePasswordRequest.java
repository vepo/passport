package dev.vepo.passport.auth.password.change;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@PasswordNotEqual
public record ChangePasswordRequest(@NotBlank String currentPassword,
                                    @NotBlank @Size(min = 8, max = 20) String newPassword) {}
