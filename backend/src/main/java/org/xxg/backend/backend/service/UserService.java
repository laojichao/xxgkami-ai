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

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordUtil passwordUtil;

    public UserService(UserRepository userRepository, PasswordUtil passwordUtil) {
        this.userRepository = userRepository;
        this.passwordUtil = passwordUtil;
    }

    public User getUserById(Integer id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new BusinessException("用户不存在"));
    }

    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("用户不存在: " + username));
    }

    public Page<User> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    @Transactional
    public User updateProfile(Integer userId, String nickname, String email, String phone) {
        User user = getUserById(userId);
        if (nickname != null) user.setNickname(nickname);
        if (email != null) user.setEmail(email);
        if (phone != null) user.setPhone(phone);
        user.setUpdateTime(LocalDateTime.now());
        return userRepository.save(user);
    }

    @Transactional
    public void changePassword(Integer userId, String oldPassword, String newPassword) {
        User user = getUserById(userId);
        if (!passwordUtil.matches(oldPassword, user.getPassword())) {
            throw new BusinessException("原密码错误");
        }
        user.setPassword(passwordUtil.encode(newPassword));
        user.setUpdateTime(LocalDateTime.now());
        userRepository.save(user);
    }

    @Transactional
    public void toggleUserStatus(Integer userId) {
        User user = getUserById(userId);
        user.setStatus(!user.getStatus());
        user.setUpdateTime(LocalDateTime.now());
        userRepository.save(user);
    }

    @Transactional
    public void deleteUser(Integer userId) {
        User user = getUserById(userId);
        userRepository.delete(user);
    }

    public Map<String, Object> getUserStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", userRepository.count());
        stats.put("activeUsers", userRepository.countByStatus(true));
        stats.put("todayUsers", userRepository.countByCreateTimeAfter(
                LocalDateTime.now().toLocalDate().atStartOfDay()));
        return stats;
    }
}
