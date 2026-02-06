package dev.vepo.passport.profile.assignroles;

import java.util.Set;

import jakarta.validation.constraints.NotEmpty;

public record AssignRolesToProfileRequest(@NotEmpty Set<Long> roleIds) {}
