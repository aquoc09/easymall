package com.quocnva.easymall.util;

import java.security.SecureRandom;

/**
 * Generates cryptographically secure 6-digit OTP codes.
 * Replaces OtpUtils.java (which used Math.random — not cryptographically safe).
 */
public final class OtpUtil {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private OtpUtil() {}

    /** Returns a zero-padded 6-digit string, e.g. "047392". */
    public static String generateOtp() {
        return String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));
    }
}
