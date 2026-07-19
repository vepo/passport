package dev.vepo.passport.directory;

import dev.vepo.passport.model.User;

public record PublicAuthorResponse(long id, String username, String name, String description) {

    public static PublicAuthorResponse load(User user) {
        return new PublicAuthorResponse(user.getId(),
                                        user.getUsername(),
                                        user.getName(),
                                        user.getDescription() != null ? user.getDescription() : "");
    }
}
