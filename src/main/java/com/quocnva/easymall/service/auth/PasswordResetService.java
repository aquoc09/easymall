package com.quocnva.easymall.service.auth;

import com.quocnva.easymall.dtos.request.auth.*;

/**
 * Handles the forgot-password → OTP verify → reset-password flow.
 */
public interface PasswordResetService {

    void forgotPassword(ForgotPasswordRequest request, String clientIp);

    void resetPassword(ResetPasswordRequest request);
}
