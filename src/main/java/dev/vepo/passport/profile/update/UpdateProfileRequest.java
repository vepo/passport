package dev.vepo.passport.profile.update;

import java.util.Set;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

public record UpdateProfileRequest(@NotBlank String name, @NotEmpty Set<Long> roleIds) {}
