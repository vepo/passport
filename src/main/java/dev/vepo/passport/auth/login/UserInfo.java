package dev.vepo.passport.auth.login;

import java.util.Set;
import java.util.stream.Collectors;

import dev.vepo.passport.model.Profile;
import dev.vepo.passport.model.User;

public record UserInfo(Long id, String username, String name, String email, Set<String> profiles) {
    public static UserInfo load(User user) {
        return new UserInfo(user.getId(),
                            user.getUsername(),
                            user.getName(),
                            user.getEmail(),
                            user.getProfiles()
                                .stream()
                                .map(Profile::getName)
                                .collect(Collectors.toSet()));
    }
}
