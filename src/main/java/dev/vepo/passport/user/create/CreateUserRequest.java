package dev.vepo.passport.user.create;

import java.util.Set;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(@NotBlank @Size(min = 4, max = 15) @NotBlank String username,
                                @NotBlank String name,
                                @NotBlank @Email String email,
                                @NotEmpty Set<Long> profileIds) {}