package dev.vepo.passport.profile;

import java.util.Set;
import java.util.stream.Collectors;

import dev.vepo.passport.model.Profile;
import dev.vepo.passport.role.RoleResponse;

public record ProfileResponse(Long id, String name, Set<RoleResponse> roles, boolean disabled) {
    public static ProfileResponse from(Profile profile) {
        return new ProfileResponse(profile.getId(),
                                   profile.getName(),
                                   profile.getRoles()
                                          .stream()
                                          .map(RoleResponse::from)
                                          .collect(Collectors.toSet()),
                                   profile.isDisabled());
    }
}