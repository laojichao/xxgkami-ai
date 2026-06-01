package org.xxg.backend.backend.service;

import org.springframework.stereotype.Service;
import org.xxg.backend.backend.entity.SocialUser;
import org.xxg.backend.backend.entity.User;
import org.xxg.backend.backend.mapper.SocialUserRepository;
import org.xxg.backend.backend.mapper.UserRepository;
import java.util.Optional;

/**
 * OAuth社交登录服务
 * 处理第三方社交账号（如微信、QQ等）与系统用户的绑定和查找
 */
@Service
public class OAuthService {
    private final SocialUserRepository socialUserRepository;
    private final UserRepository userRepository;

    public OAuthService(SocialUserRepository socialUserRepository, UserRepository userRepository) {
        this.socialUserRepository = socialUserRepository;
        this.userRepository = userRepository;
    }

    /**
     * 根据社交平台用户ID查找系统用户
     * @param socialUid 社交平台用户唯一标识
     * @param socialType 社交平台类型（如wechat、qq等）
     * @return 匹配的系统用户，不存在返回空
     */
    public Optional<User> findBySocialUid(String socialUid, String socialType) {
        return socialUserRepository.findBySocialUidAndSocialType(socialUid, socialType)
                .map(su -> userRepository.findById(su.getUserId()).orElse(null));
    }

    /**
     * 绑定社交账号到系统用户
     * @param userId 系统用户ID
     * @param socialUid 社交平台用户唯一标识
     * @param socialType 社交平台类型
     * @return 绑定后的系统用户
     */
    public User bindSocialUser(Integer userId, String socialUid, String socialType) {
        SocialUser su = new SocialUser();
        su.setUserId(userId);
        su.setSocialUid(socialUid);
        su.setSocialType(socialType);
        socialUserRepository.save(su);
        return userRepository.findById(userId).orElse(null);
    }
}
