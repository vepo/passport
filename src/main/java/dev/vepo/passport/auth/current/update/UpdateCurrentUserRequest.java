package dev.vepo.passport.auth.current.update;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateCurrentUserRequest(@NotBlank @Size(min = 2, max = 100) String name,
                                       @NotBlank @Email @Size(max = 255) String email,
                                       @Size(max = 2000) String description) {}
