package dev.vepo.passport.role;

import dev.vepo.passport.model.Role;

public record RoleResponse(Long id, String name) {
    public static RoleResponse from(Role role) {
        return new RoleResponse(role.getId(), role.getName());
    }
}