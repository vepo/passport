package dev.vepo.passport.auth.login;

import dev.vepo.passport.model.User;

public record LoginResponse(String token, UserInfo user) {
    public static LoginResponse load(String token, User user) {
        return new LoginResponse(token, UserInfo.load(user));
    }
}
