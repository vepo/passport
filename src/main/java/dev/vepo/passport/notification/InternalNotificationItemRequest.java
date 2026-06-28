package dev.vepo.passport.notification;

import jakarta.validation.constraints.NotBlank;

public record InternalNotificationItemRequest(@NotBlank String title,
                                              String description,
                                              String report) {}
