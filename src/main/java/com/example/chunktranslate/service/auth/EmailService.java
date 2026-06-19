package com.example.chunktranslate.service.auth;

public interface EmailService {

    void sendVerificationCode(String to, String code);

    void sendEmail(String to, String subject, String content);
}