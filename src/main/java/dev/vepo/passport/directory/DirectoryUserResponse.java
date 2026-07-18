package dev.vepo.passport.directory;

import dev.vepo.passport.model.User;

public record DirectoryUserResponse(long id, String username, String name, String email) {

    public static DirectoryUserResponse load(User user) {
        return new DirectoryUserResponse(user.getId(), user.getUsername(), user.getName(), user.getEmail());
    }
}
