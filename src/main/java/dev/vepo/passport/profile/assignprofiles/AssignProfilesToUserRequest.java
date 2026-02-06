package dev.vepo.passport.profile.assignprofiles;

import java.util.Set;

import jakarta.validation.constraints.NotEmpty;

public record AssignProfilesToUserRequest(@NotEmpty Set<Long> profileIds) {}
