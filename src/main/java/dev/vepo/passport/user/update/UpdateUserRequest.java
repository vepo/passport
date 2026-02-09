package dev.vepo.passport.user.update;

import java.util.Set;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(@NotBlank @Size(min = 2, max = 100) String name,
                                @NotBlank @Email @Size(max = 255) String email,
                                Set<Long> profileIds) {}