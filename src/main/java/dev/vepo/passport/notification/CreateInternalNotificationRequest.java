package dev.vepo.passport.notification;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateInternalNotificationRequest(@NotBlank String sourceService,
                                                @NotBlank String sourceType,
                                                Long engageChannelId,
                                                @NotBlank String title,
                                                String description,
                                                String report,
                                                @Valid List<InternalNotificationItemRequest> items) {}
