package com.quocnva.easymall.util;

/**
 * Centralized OTP and token-related constants used across multiple services.
 */
public final class OtpConstants {

    /** OTP validity period — 5 minutes. */
    public static final long OTP_TTL_SECONDS = 300L;

    /** OTP rate-limit cooldown — 1 minute between resends. */
    public static final long OTP_RATE_TTL_SECONDS = 60L;

    /** Refresh Token lifetime — 7 days. */
    public static final long RT_TTL_DAYS = 7L;

    private OtpConstants() {}
}
