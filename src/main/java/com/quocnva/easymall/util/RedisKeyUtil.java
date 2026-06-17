package com.quocnva.easymall.util;

import com.quocnva.easymall.enums.OtpType;

/**
 * Centralizes all Redis key patterns to avoid magic strings scattered across services.
 *
 * Key patterns:
 *   otp:{TYPE}:{email}           — OTP value (TTL 5 min)
 *   pending_user:{email}         — JSON of pending RegisterRequest (TTL 5 min)
 *   blacklist:at:{jti}           — Revoked Access Token JTI (TTL = remaining AT lifetime)
 *   otp_rate:{TYPE}:{email}      — Rate-limit sentinel (TTL 60 s)
 */
public final class RedisKeyUtil {

    private RedisKeyUtil() {}

    public static String otpKey(OtpType type, String email) {
        return "otp:" + type.name() + ":" + email;
    }

    public static String pendingUserKey(String email) {
        return "pending_user:" + email;
    }

    public static String blacklistAtKey(String jti) {
        return "blacklist:at:" + jti;
    }

    public static String otpRateKey(OtpType type, String email) {
        return "otp_rate:" + type.name() + ":" + email;
    }
}
