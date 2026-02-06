package dev.vepo.passport.profile.create;

import java.util.Set;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

public record CreateProfileRequest(@NotBlank String name, @NotEmpty Set<Long> roleIds) {}
