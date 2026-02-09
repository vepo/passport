package dev.vepo.passport.user;

import dev.vepo.passport.model.Profile;

public record ProfileInfoResponse(long id, String name) {
    public static ProfileInfoResponse load(Profile profile) {
        return new ProfileInfoResponse(profile.getId(), profile.getName());
    }
}
