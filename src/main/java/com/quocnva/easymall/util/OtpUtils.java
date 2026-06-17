package com.quocnva.easymall.util;

/**
 * Utility helpers for generating OTP codes.
 */
public final class OtpUtils {

    private OtpUtils() {}

    /**
     * Generates a zero-padded 6-digit OTP string.
     */
    public static String generate6DigitOtp() {
        int otp = (int) (Math.random() * 1_000_000);
        return String.format("%06d", otp);
    }
}
