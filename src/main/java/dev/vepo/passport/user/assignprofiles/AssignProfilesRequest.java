package dev.vepo.passport.user.assignprofiles;

import java.util.Set;

import jakarta.validation.constraints.NotEmpty;

public record AssignProfilesRequest(
    @NotEmpty(message = "At least one profile must be assigned")
    Set<Long> profileIds
) {}