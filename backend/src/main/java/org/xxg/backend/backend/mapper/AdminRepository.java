package org.xxg.backend.backend.mapper;

import org.springframework.data.jpa.repository.JpaRepository;
import org.xxg.backend.backend.entity.Admin;
import java.util.Optional;

/**
 * 管理员数据访问接口
 * 提供管理员账户的增删改查及按用户名查询功能
 */
public interface AdminRepository extends JpaRepository<Admin, Integer> {
    /** 根据用户名查询管理员 */
    Optional<Admin> findByUsername(String username);
    /** 判断指定用户名的管理员是否存在 */
    boolean existsByUsername(String username);
}
