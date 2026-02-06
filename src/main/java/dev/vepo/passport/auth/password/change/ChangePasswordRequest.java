package dev.vepo.passport.auth.password.change;

public record ChangePasswordRequest(String currentPassword, String newPassword) {}
