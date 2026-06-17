package com.quocnva.easymall.service.impl;

import com.quocnva.easymall.enums.OtpType;
import com.quocnva.easymall.service.EmailService;
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
            case ACTIVATION      -> "[EasyMall] Kích hoạt tài khoản của bạn";
            case FORGOT_PASSWORD -> "[EasyMall] Đặt lại mật khẩu";
        };
    }

    private String buildBody(String otp, OtpType type) {
        return switch (type) {
            case ACTIVATION -> """
                    Xin chào!
                    
                    Mã OTP kích hoạt tài khoản EasyMall của bạn là: %s
                    
                    Mã này có hiệu lực trong 5 phút. Vui lòng không chia sẻ mã này với bất kỳ ai.
                    
                    Trân trọng,
                    Đội ngũ EasyMall
                    """.formatted(otp);
            case FORGOT_PASSWORD -> """
                    Xin chào!
                    
                    Mã OTP đặt lại mật khẩu EasyMall của bạn là: %s
                    
                    Mã này có hiệu lực trong 5 phút. Nếu bạn không yêu cầu đặt lại mật khẩu, hãy bỏ qua email này.
                    
                    Trân trọng,
                    Đội ngũ EasyMall
                    """.formatted(otp);
        };
    }
}
