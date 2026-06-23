package com.quocnva.easymall.util;

import jakarta.servlet.http.HttpServletRequest;
import lombok.experimental.UtilityClass;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@UtilityClass
public class DeviceSessionUtil {

    /**
     * Lấy IP thực của client.
     * Ưu tiên X-Forwarded-For (qua proxy/load balancer), fallback về RemoteAddr.
     */
    public String extractIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    /** Lấy User-Agent header */
    public String extractUserAgent(HttpServletRequest request) {
        String ua = request.getHeader("User-Agent");
        return ua != null ? ua : "unknown";
    }

    /**
     * Lấy device fingerprint từ header custom X-Device-Fingerprint.
     * Frontend (canvas/WebGL fingerprint) gửi lên trong header này.
     */
    public String extractFingerprint(HttpServletRequest request) {
        return request.getHeader("X-Device-Fingerprint");
    }

    /**
     * Tạo sessionId duy nhất từ IP + UserAgent bằng SHA-256.
     * Dùng để lookup / tái sử dụng session hiện có.
     */
    public String buildSessionId(String ip, String userAgent) {
        try {
            String raw = ip + "|" + userAgent;
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 luôn tồn tại trong JDK — không bao giờ xảy ra
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}
