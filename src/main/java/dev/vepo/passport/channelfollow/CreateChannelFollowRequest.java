package dev.vepo.passport.channelfollow;

import jakarta.validation.constraints.NotNull;

public record CreateChannelFollowRequest(@NotNull Long engageChannelId) {}
