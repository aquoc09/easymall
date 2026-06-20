package com.quocnva.easymall.service.email.impl;

import com.quocnva.easymall.enums.OtpType;
import com.quocnva.easymall.service.email.EmailService;
import com.quocnva.easymall.util.Translator;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Override
    public void sendOtpEmail(String toEmail, String otp, OtpType type) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject(buildSubject(type));
        message.setText(buildBody(otp, type));
        mailSender.send(message);
    }

    private String buildSubject(OtpType type) {
        return switch (type) {
            case ACTIVATION      -> Translator.toLocale("email.activation.subject");
            case FORGOT_PASSWORD -> Translator.toLocale("email.forgot-password.subject");
        };
    }

    private String buildBody(String otp, OtpType type) {
        return switch (type) {
            case ACTIVATION      -> Translator.toLocale("email.activation.body", otp);
            case FORGOT_PASSWORD -> Translator.toLocale("email.forgot-password.body", otp);
        };
    }
}
