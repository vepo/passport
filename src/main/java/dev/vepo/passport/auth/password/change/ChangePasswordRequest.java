package dev.vepo.passport.auth.password.change;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@PasswordNotEqual(message = "New password must be different from current password")
public record ChangePasswordRequest(@NotBlank String currentPassword,
                                    @NotBlank @Size(min = 8, message = "Password must be at least 8 characters long") String newPassword) {}
