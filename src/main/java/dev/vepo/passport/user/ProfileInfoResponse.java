package dev.vepo.passport.user;

import dev.vepo.passport.model.Profile;

public record ProfileInfoResponse() {
    public static ProfileInfoResponse load(Profile profile) {
        return new ProfileInfoResponse();
    }
}
