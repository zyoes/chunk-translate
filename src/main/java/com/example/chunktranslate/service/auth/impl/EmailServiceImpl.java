package com.example.chunktranslate.service.auth.impl;

import com.example.chunktranslate.service.auth.EmailService;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/**
 * 邮件发送服务实现。
 * <p>通过 {@link org.springframework.mail.javamail.JavaMailSender} 发送 HTML 格式邮件，
 * 发件人地址自动使用 application.yml 中配置的 spring.mail.username。</p>
 */
@Slf4j
@Service
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender sender;
    private final String from;

    public EmailServiceImpl(JavaMailSender sender,
            @Value("${spring.mail.username}") String from) {
        this.sender = sender;
        this.from = from;
    }

    @Override
    public void sendVerificationCode(String to, String code) {
        sendEmail(to, "AI 文档翻译平台 - 邮箱验证码",
                "本次操作的验证码是：" + code + "（5分钟之内有效）");
    }

    @Override
    public void sendEmail(String to, String subject, String content) {
        MimeMessage message = sender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message);
        try {
            helper.setFrom(new InternetAddress(from));
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(content, true);
            sender.send(message);
            log.info("邮件已发送: to={}", to);
        } catch (Exception e) {
            log.error("邮件发送失败: to={}", to, e);
        }
    }
}