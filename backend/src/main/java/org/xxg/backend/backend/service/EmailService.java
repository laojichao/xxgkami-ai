package org.xxg.backend.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    @Value("${spring.mail.properties.mail.smtp.auth:false}")
    private String smtpAuth;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Async
    public void sendVerificationCode(String to, String code, String type) {
        try {
            if (fromEmail == null || fromEmail.isEmpty()) {
                log.warn("[EMAIL] Mail not configured. Code for {}: {}", to, code);
                return;
            }
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject("register".equals(type) ? "注册验证码" : "重置密码验证码");
            message.setText("您的验证码是: " + code + "\n有效期10分钟，请勿泄露给他人。");
            mailSender.send(message);
        } catch (Exception e) {
            log.error("[EMAIL] Failed to send to {}: {}", to, e.getMessage());
        }
    }

    @Async
    public void sendOrderNotification(String to, String orderNo, String content) {
        try {
            if (fromEmail == null || fromEmail.isEmpty()) return;
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject("订单通知 - " + orderNo);
            message.setText(content);
            mailSender.send(message);
        } catch (Exception e) {
            log.error("[EMAIL] Failed to send order notification to {}: {}", to, e.getMessage());
        }
    }

    @Async
    public void sendCardKeys(String to, String cardKeys) {
        try {
            if (fromEmail == null || fromEmail.isEmpty()) return;
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject("卡密发放通知");
            message.setText("您购买的卡密如下:\n\n" + cardKeys + "\n\n请妥善保管。");
            mailSender.send(message);
        } catch (Exception e) {
            log.error("[EMAIL] Failed to send card keys to {}: {}", to, e.getMessage());
        }
    }
}
