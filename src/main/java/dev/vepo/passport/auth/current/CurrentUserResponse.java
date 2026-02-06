package dev.vepo.passport.auth.current;

import java.util.Set;
import java.util.stream.Collectors;

import dev.vepo.passport.model.Role;
import dev.vepo.passport.model.User;

public record CurrentUserResponse(long id, String username, String name, String email, Set<String> roles) {

    public static CurrentUserResponse load(User user) {
        return new CurrentUserResponse(user.getId(),
                                       user.getUsername(),
                                       user.getName(),
                                       user.getEmail(),
                                       user.getProfiles()
                                           .stream()
                                           .flatMap(profile -> profile.getRoles().stream())
                                           .distinct()
                                           .map(Role::getName)
                                           .collect(Collectors.toSet()));
    }
}