package dev.vepo.passport.profile.assignroles;

import java.util.Set;

import jakarta.validation.constraints.NotEmpty;

public record AssignRolesRequest(@NotEmpty(message = "At least one role must be assigned") Set<Long> roleIds) {}