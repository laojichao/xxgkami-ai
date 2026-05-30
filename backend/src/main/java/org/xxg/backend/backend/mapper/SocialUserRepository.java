package org.xxg.backend.backend.mapper;

import org.springframework.data.jpa.repository.JpaRepository;
import org.xxg.backend.backend.entity.SocialUser;
import java.util.Optional;

public interface SocialUserRepository extends JpaRepository<SocialUser, Integer> {
    Optional<SocialUser> findBySocialUidAndSocialType(String socialUid, String socialType);
    Optional<SocialUser> findByUserId(Integer userId);
}
