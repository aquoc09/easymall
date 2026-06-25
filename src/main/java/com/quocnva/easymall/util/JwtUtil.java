package com.quocnva.easymall.util;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.*;
import com.nimbusds.jwt.*;
import com.quocnva.easymall.config.JwtConfig;
import com.quocnva.easymall.entity.UserEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * JWT utility — generates and parses HMAC-SHA256 signed JWTs.
 *
 * Access Token claims: sub, jti, iss, iat, exp, scope (role), type=ACCESS
 * Refresh Token claims: sub, jti, iss, iat, exp, type=REFRESH
 *
 * Uses Nimbus JOSE (already on classpath via spring-security-oauth2-jose).
 */
@Component
@RequiredArgsConstructor
public class JwtUtil {

    private static final String ISSUER = "easymall";
    private static final String CLAIM_TYPE = "type";
    private static final String CLAIM_SCOPE = "scope";
    private static final String TYPE_ACCESS = "ACCESS";
    private static final String TYPE_REFRESH = "REFRESH";

    private final JwtConfig jwtConfig;

    // ── Token Generation ─────────────────────────────────────────────────

    public String generateAccessToken(UserEntity user) {
        try {
            Instant now = Instant.now();
            Instant exp = now.plusMillis(jwtConfig.getValidDuration());

            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .subject(user.getEmail())
                    .jwtID(UUID.randomUUID().toString())
                    .issuer(ISSUER)
                    .issueTime(Date.from(now))
                    .expirationTime(Date.from(exp))
                    .claim(CLAIM_SCOPE, resolveScope(user))
                    .claim(CLAIM_TYPE, TYPE_ACCESS)
                    .build();

            return sign(claims);
        } catch (JOSEException e) {
            throw new RuntimeException("Failed to generate access token", e);
        }
    }

    public String generateRefreshToken(UserEntity user) {
        try {
            Instant now = Instant.now();
            Instant exp = now.plusMillis(jwtConfig.getRefreshableDuration());

            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .subject(user.getEmail())
                    .jwtID(UUID.randomUUID().toString())
                    .issuer(ISSUER)
                    .issueTime(Date.from(now))
                    .expirationTime(Date.from(exp))
                    .claim(CLAIM_TYPE, TYPE_REFRESH)
                    .build();

            return sign(claims);
        } catch (JOSEException e) {
            throw new RuntimeException("Failed to generate refresh token", e);
        }
    }

    // ── Token Parsing & Claims ────────────────────────────────────────────

    /**
     * Verifies HMAC signature and returns the parsed SignedJWT.
     * Throws RuntimeException if signature is invalid.
     */
    public SignedJWT parseToken(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            MACVerifier verifier = new MACVerifier(jwtConfig.getSignerKey().getBytes());
            if (!signedJWT.verify(verifier)) {
                throw new RuntimeException("Invalid JWT signature");
            }
            return signedJWT;
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse token: " + e.getMessage(), e);
        }
    }

    /** Extracts the JTI (JWT ID) claim — used as the Redis AT blacklist key. */
    public String getJti(String token) {
        try {
            return parseToken(token).getJWTClaimsSet().getJWTID();
        } catch (Exception e) {
            throw new RuntimeException("Failed to extract JTI", e);
        }
    }

    /**
     * Returns remaining lifetime of the token in seconds.
     * Used to set the Redis AT blacklist TTL exactly, so the key auto-expires.
     */
    public long getRemainingTtlSeconds(String token) {
        try {
            Date exp = parseToken(token).getJWTClaimsSet().getExpirationTime();
            long remaining = (exp.getTime() - System.currentTimeMillis()) / 1000;
            return Math.max(remaining, 0);
        } catch (Exception e) {
            return 0;
        }
    }

    // ── Private Helpers ───────────────────────────────────────────────────

    private String sign(JWTClaimsSet claims) throws JOSEException {
        JWSHeader header = new JWSHeader(JWSAlgorithm.HS256);
        SignedJWT jwt = new SignedJWT(header, claims);
        jwt.sign(new MACSigner(jwtConfig.getSignerKey().getBytes()));
        return jwt.serialize();
    }

    private String resolveScope(UserEntity user) {
        return (user.getRole() != null) ? user.getRole().getRoleName() : "";
    }
}
