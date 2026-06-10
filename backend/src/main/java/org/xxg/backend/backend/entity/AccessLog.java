package org.xxg.backend.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 访问日志实体 - 记录系统所有HTTP请求的访问信息
 */
@Data
@Entity
@Table(name = "access_logs", indexes = {
    @Index(name = "idx_alog_ip", columnList = "ip"),
    @Index(name = "idx_alog_time", columnList = "create_time")
})
public class AccessLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 50)
    private String ip; // 请求来源IP地址

    @Column(length = 10)
    private String method; // HTTP请求方法(GET/POST等)

    @Column(length = 500)
    private String uri; // 请求的URI路径

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent; // 客户端浏览器/设备信息

    private Integer status; // HTTP响应状态码

    @Column(name = "duration_ms")
    private Long durationMs; // 请求处理耗时(毫秒)

    @Column(length = 50)
    private String username; // 请求关联的用户名(可为空)

    @Column(name = "create_time")
    private LocalDateTime createTime = LocalDateTime.now(); // 记录创建时间
}
