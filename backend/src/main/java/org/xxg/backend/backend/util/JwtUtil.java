package org.xxg.backend.backend.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT令牌工具类
 * 提供JWT令牌的生成、验证和解析功能，支持Access Token和Refresh Token两种类型。
 * 密钥和过期时间通过application.yml配置注入。
 */
@Component
public class JwtUtil {

    /** JWT签名密钥，从配置文件注入 */
    @Value("${jwt.secret}")
    private String secret;

    /** Access Token过期时间（毫秒） */
    @Value("${jwt.access-token-expiration}")
    private long accessTokenExpiration;

    /** Refresh Token过期时间（毫秒） */
    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    /**
     * 根据配置的密钥字符串生成HMAC-SHA签名密钥
     */
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 生成访问令牌
     * @param username 用户名（作为令牌主题）
     * @param role 用户角色
     * @return 签名后的JWT访问令牌字符串
     */
    public String generateAccessToken(String username, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", role);
        claims.put("type", "access");
        return createToken(claims, username, accessTokenExpiration);
    }

    /**
     * 生成刷新令牌
     * @param username 用户名（作为令牌主题）
     * @param role 用户角色
     * @return 签名后的JWT刷新令牌字符串
     */
    public String generateRefreshToken(String username, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", role);
        claims.put("type", "refresh");
        return createToken(claims, username, refreshTokenExpiration);
    }

    /**
     * 创建JWT令牌的通用方法
     * @param claims 自定义声明（如角色、令牌类型）
     * @param subject 令牌主题（通常是用户名）
     * @param expiration 有效时长（毫秒）
     * @return 签名后的JWT字符串
     */
    private String createToken(Map<String, Object> claims, String subject, long expiration) {
        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * 从令牌中提取用户名
     * @param token JWT令牌字符串
     * @return 用户名
     */
    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    /**
     * 从令牌中提取用户角色
     * @param token JWT令牌字符串
     * @return 角色名称
     */
    public String extractRole(String token) {
        return extractAllClaims(token).get("role", String.class);
    }

    /**
     * 从令牌中提取令牌类型（access/refresh）
     * @param token JWT令牌字符串
     * @return 令牌类型
     */
    public String extractTokenType(String token) {
        return extractAllClaims(token).get("type", String.class);
    }

    /**
     * 验证令牌是否有效（签名正确且未过期）
     * @param token JWT令牌字符串
     * @return 有效返回true，否则返回false
     */
    public boolean isTokenValid(String token) {
        try {
            Claims claims = extractAllClaims(token);
            return !claims.getExpiration().before(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 判断是否为访问令牌
     * @param token JWT令牌字符串
     * @return 是访问令牌返回true
     */
    public boolean isAccessToken(String token) {
        try {
            return "access".equals(extractTokenType(token));
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 判断是否为刷新令牌
     * @param token JWT令牌字符串
     * @return 是刷新令牌返回true
     */
    public boolean isRefreshToken(String token) {
        try {
            return "refresh".equals(extractTokenType(token));
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 解析令牌并提取所有声明
     * @param token JWT令牌字符串
     * @return Claims对象，包含所有声明信息
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
