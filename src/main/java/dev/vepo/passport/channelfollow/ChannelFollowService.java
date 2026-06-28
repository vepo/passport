package dev.vepo.passport.channelfollow;

import java.util.List;

import dev.vepo.passport.model.ChannelFollow;
import dev.vepo.passport.model.User;
import dev.vepo.passport.user.UserRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;

@ApplicationScoped
public class ChannelFollowService {

    private final ChannelFollowRepository channelFollowRepository;
    private final UserRepository userRepository;

    @Inject
    public ChannelFollowService(ChannelFollowRepository channelFollowRepository, UserRepository userRepository) {
        this.channelFollowRepository = channelFollowRepository;
        this.userRepository = userRepository;
    }

    public List<ChannelFollowResponse> listForUser(String username) {
        var user = requireActiveUser(username);
        return channelFollowRepository.findByUser(user)
                                      .stream()
                                      .map(ChannelFollowResponse::from)
                                      .toList();
    }

    @Transactional
    public ChannelFollowResponse follow(String username, Long engageChannelId) {
        var user = requireActiveUser(username);
        return channelFollowRepository.findByUserAndEngageChannelId(user, engageChannelId)
                                      .map(ChannelFollowResponse::from)
                                      .orElseGet(() -> ChannelFollowResponse.from(channelFollowRepository.save(new ChannelFollow(user,
                                                                                                                                 engageChannelId))));
    }

    @Transactional
    public void unfollow(String username, Long engageChannelId) {
        var user = requireActiveUser(username);
        var follow = channelFollowRepository.findByUserAndEngageChannelId(user, engageChannelId)
                                            .orElseThrow(() -> new NotFoundException("Channel follow not found for channel id: %d".formatted(engageChannelId)));
        channelFollowRepository.delete(follow);
    }

    public boolean isFollowing(String username, Long engageChannelId) {
        var user = requireActiveUser(username);
        return channelFollowRepository.existsByUserAndEngageChannelId(user, engageChannelId);
    }

    private User requireActiveUser(String username) {
        return userRepository.findActiveByUsername(username)
                             .orElseThrow(() -> new NotFoundException("User not found"));
    }
}
