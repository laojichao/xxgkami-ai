package org.xxg.backend.backend.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.xxg.backend.backend.entity.User;
import org.xxg.backend.backend.exception.BusinessException;
import org.xxg.backend.backend.mapper.UserRepository;
import org.xxg.backend.backend.util.PasswordUtil;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 用户管理服务
 * 提供用户注册、查询、修改资料、修改密码、启用禁用等用户管理功能
 */
@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordUtil passwordUtil;

    public UserService(UserRepository userRepository, PasswordUtil passwordUtil) {
        this.userRepository = userRepository;
        this.passwordUtil = passwordUtil;
    }

    /**
     * 根据用户ID获取用户信息
     * @param id 用户ID
     * @return 用户实体，不存在则抛出异常
     */
    @Transactional(readOnly = true)
    public User getUserById(Integer id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new BusinessException("用户不存在"));
    }

    /**
     * 根据用户名获取用户信息
     * @param username 用户名
     * @return 用户实体，不存在则抛出异常
     */
    @Transactional(readOnly = true)
    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("用户不存在"));
    }

    /**
     * 分页查询所有用户
     * @param pageable 分页参数
     * @return 用户分页结果
     */
    @Transactional(readOnly = true)
    public Page<User> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    /**
     * 更新用户个人资料
     * @param userId 用户ID
     * @param nickname 新昵称，为null则不更新
     * @param email 新邮箱，为null则不更新
     * @param phone 新手机号，为null则不更新
     * @return 更新后的用户实体
     */
    @Transactional
    public User updateProfile(Integer userId, String nickname, String email, String phone) {
        User user = getUserById(userId);
        if (nickname != null) user.setNickname(nickname);
        if (email != null && !email.equals(user.getEmail())) {
            // 检查新邮箱是否已被其他用户占用
            if (userRepository.existsByEmail(email)) {
                throw new BusinessException("该邮箱已被其他用户使用");
            }
            user.setEmail(email);
        }
        if (phone != null && !phone.equals(user.getPhone())) {
            // 检查新手机号是否已被其他用户占用
            if (userRepository.existsByPhone(phone)) {
                throw new BusinessException("该手机号已被其他用户使用");
            }
            user.setPhone(phone);
        }
        user.setUpdateTime(LocalDateTime.now());
        return userRepository.save(user);
    }

    /**
     * 修改用户密码
     * @param userId 用户ID
     * @param oldPassword 旧密码
     * @param newPassword 新密码
     */
    @Transactional
    public void changePassword(Integer userId, String oldPassword, String newPassword) {
        User user = getUserById(userId);
        if (!passwordUtil.matches(oldPassword, user.getPassword())) {
            throw new BusinessException("原密码错误");
        }
        // 密码强度校验：至少6位
        if (newPassword == null || newPassword.length() < 6) {
            throw new BusinessException("新密码长度不能少于6位");
        }
        user.setPassword(passwordUtil.encode(newPassword));
        user.setUpdateTime(LocalDateTime.now());
        userRepository.save(user);
    }

    /**
     * 切换用户启用/禁用状态
     * @param userId 用户ID
     */
    @Transactional
    public void toggleUserStatus(Integer userId) {
        User user = getUserById(userId);
        user.setStatus(!user.getStatus());
        user.setUpdateTime(LocalDateTime.now());
        userRepository.save(user);
    }

    /**
     * 删除用户
     * @param userId 用户ID
     */
    @Transactional
    public void deleteUser(Integer userId) {
        User user = getUserById(userId);
        userRepository.delete(user);
    }

    /**
     * 获取用户统计数据
     * @return 包含总用户数、活跃用户数、今日新增用户数的Map
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getUserStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", userRepository.count());
        stats.put("activeUsers", userRepository.countByStatus(true));
        stats.put("todayUsers", userRepository.countByCreateTimeAfter(
                LocalDateTime.now().toLocalDate().atStartOfDay()));
        return stats;
    }
}
