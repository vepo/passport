package dev.vepo.passport.user;

import java.time.Instant;
import java.util.Set;
import java.util.stream.Collectors;

import dev.vepo.passport.model.User;

public record UserResponse(long id,
                           String username,
                           String name,
                           String email,
                           Set<ProfileInfoResponse> profiles,
                           boolean deleted,
                           Instant createdAt,
                           Instant updatedAt) {

    public static UserResponse load(User user) {
        return new UserResponse(user.getId(),
                                user.getUsername(),
                                user.getName(),
                                user.getEmail(),
                                user.getProfiles()
                                    .stream()
                                    .map(ProfileInfoResponse::load)
                                    .collect(Collectors.toSet()),
                                user.isDeleted(),
                                user.getCreatedAt(),
                                user.getUpdatedAt());
    }
}
