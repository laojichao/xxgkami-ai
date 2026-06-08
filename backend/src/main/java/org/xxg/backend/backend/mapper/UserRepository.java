package org.xxg.backend.backend.mapper;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.xxg.backend.backend.entity.User;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 用户数据访问接口
 * 提供用户的增删改查、按用户名/邮箱查询及用户统计功能
 */
public interface UserRepository extends JpaRepository<User, Integer> {
    /** 根据用户名查询用户 */
    Optional<User> findByUsername(String username);
    /** 根据邮箱查询用户 */
    Optional<User> findByEmail(String email);
    /** 判断指定用户名是否存在 */
    boolean existsByUsername(String username);
    /** 判断指定邮箱是否存在 */
    boolean existsByEmail(String email);
    /** 判断指定手机号是否存在 */
    boolean existsByPhone(String phone);
    /** 统计指定时间之后注册的用户数量 */
    long countByCreateTimeAfter(LocalDateTime time);
    /** 统计指定状态的用户数量 */
    long countByStatus(Boolean status);
}
