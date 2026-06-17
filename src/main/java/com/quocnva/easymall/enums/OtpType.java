package com.quocnva.easymall.enums;

/**
 * Discriminates OTP purpose — used in Redis key namespacing and email templating.
 */
public enum OtpType {
    ACTIVATION,      // 6-digit OTP for new account registration
    FORGOT_PASSWORD  // 6-digit OTP for password reset
}
