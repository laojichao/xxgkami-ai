package org.xxg.backend.backend.mapper;

import org.springframework.data.jpa.repository.JpaRepository;
import org.xxg.backend.backend.entity.SocialUser;
import java.util.Optional;

/**
 * 社交用户数据访问接口
 * 提供社交账号绑定信息的增删改查及按社交UID/用户ID查询功能
 */
public interface SocialUserRepository extends JpaRepository<SocialUser, Integer> {
    /** 根据社交平台UID和社交类型查询绑定记录 */
    Optional<SocialUser> findBySocialUidAndSocialType(String socialUid, String socialType);
    /** 根据用户ID查询社交绑定记录 */
    Optional<SocialUser> findByUserId(Integer userId);
    /** 根据用户ID删除社交绑定记录 */
    void deleteByUserId(Integer userId);
}
