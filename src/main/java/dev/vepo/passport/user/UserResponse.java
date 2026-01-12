package dev.vepo.passport.user;

import java.time.Instant;
import java.util.List;

public record UserResponse(long id, String username, String name, String email, List<String> roles, boolean deleted, Instant createdAt, Instant updatedAt) {

    public static UserResponse load(User user) {
        return new UserResponse(user.getId(),
                                user.getUsername(),
                                user.getName(),
                                user.getEmail(),
                                user.getRoles()
                                    .stream()
                                    .map(Role::role)
                                    .toList(),
                                user.isDeleted(),
                                user.getCreatedAt(),
                                user.getUpdatedAt());
    }
}
