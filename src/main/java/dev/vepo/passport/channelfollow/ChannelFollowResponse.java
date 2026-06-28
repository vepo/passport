package dev.vepo.passport.channelfollow;

import java.time.Instant;

import dev.vepo.passport.model.ChannelFollow;

public record ChannelFollowResponse(Long id, Long engageChannelId, Instant createdAt) {
    public static ChannelFollowResponse from(ChannelFollow channelFollow) {
        return new ChannelFollowResponse(channelFollow.getId(),
                                         channelFollow.getEngageChannelId(),
                                         channelFollow.getCreatedAt());
    }
}
