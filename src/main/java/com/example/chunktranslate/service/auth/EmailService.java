package com.example.chunktranslate.service.auth;

/**
 * 邮件发送服务接口。
 * <p>通过 SMTP 发送邮件，当前用于发送邮箱验证码。</p>
 */
public interface EmailService {

    void sendVerificationCode(String to, String code);

    void sendEmail(String to, String subject, String content);
}