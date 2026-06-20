package com.quocnva.easymall.service.email;

import com.quocnva.easymall.enums.OtpType;

public interface EmailService {

    /**
     * Sends a 6-digit OTP email.
     * Subject and body are templated by OtpType:
     *   ACTIVATION      → account activation instructions
     *   FORGOT_PASSWORD → password reset instructions
     */
    void sendOtpEmail(String toEmail, String otp, OtpType type);
}
