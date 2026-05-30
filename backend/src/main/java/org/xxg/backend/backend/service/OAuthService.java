package org.xxg.backend.backend.service;

import org.springframework.stereotype.Service;
import org.xxg.backend.backend.entity.SocialUser;
import org.xxg.backend.backend.entity.User;
import org.xxg.backend.backend.mapper.SocialUserRepository;
import org.xxg.backend.backend.mapper.UserRepository;
import java.util.Optional;

@Service
public class OAuthService {
    private final SocialUserRepository socialUserRepository;
    private final UserRepository userRepository;

    public OAuthService(SocialUserRepository socialUserRepository, UserRepository userRepository) {
        this.socialUserRepository = socialUserRepository;
        this.userRepository = userRepository;
    }

    public Optional<User> findBySocialUid(String socialUid, String socialType) {
        return socialUserRepository.findBySocialUidAndSocialType(socialUid, socialType)
                .map(su -> userRepository.findById(su.getUserId()).orElse(null));
    }

    public User bindSocialUser(Integer userId, String socialUid, String socialType) {
        SocialUser su = new SocialUser();
        su.setUserId(userId);
        su.setSocialUid(socialUid);
        su.setSocialType(socialType);
        socialUserRepository.save(su);
        return userRepository.findById(userId).orElse(null);
    }
}
