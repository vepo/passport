package dev.vepo.passport.user.update;

import java.util.Set;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(@NotBlank @Size String name,
                                @NotBlank @Email @Size String email,
                                Set<Long> profileIds) {}