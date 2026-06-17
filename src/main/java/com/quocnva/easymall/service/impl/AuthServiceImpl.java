package com.quocnva.easymall.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jwt.SignedJWT;
import com.quocnva.easymall.dtos.request.auth.*;
import com.quocnva.easymall.dtos.response.auth.AuthResponse;
import com.quocnva.easymall.dtos.response.auth.IntrospectResponse;
import com.quocnva.easymall.dtos.response.auth.UserResponse;
import com.quocnva.easymall.entity.RoleEntity;
import com.quocnva.easymall.entity.TokenEntity;
import com.quocnva.easymall.entity.UserEntity;
import com.quocnva.easymall.enums.OtpType;
import com.quocnva.easymall.exception.AppException;
import com.quocnva.easymall.exception.ErrorCode;
import com.quocnva.easymall.repository.RoleRepository;
import com.quocnva.easymall.repository.TokenRepository;
import com.quocnva.easymall.repository.UserRepository;
import com.quocnva.easymall.service.AuthService;
import com.quocnva.easymall.service.EmailService;
import com.quocnva.easymall.util.JwtUtil;
import com.quocnva.easymall.util.OtpUtil;
import com.quocnva.easymall.util.RedisKeyUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final long OTP_TTL_SECONDS      = 300L;  // 5 min
    private static final long OTP_RATE_TTL_SECONDS = 60L;   // 1 min cool-down
    private static final long RT_TTL_DAYS          = 7L;

    private final UserRepository              userRepository;
    private final TokenRepository             tokenRepository;
    private final RoleRepository              roleRepository;
    private final PasswordEncoder             passwordEncoder;
    private final JwtUtil                     jwtUtil;
    private final EmailService                emailService;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper                objectMapper;

    // ── Register ─────────────────────────────────────────────────────────

    @Override
    public void register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AppException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        // Store pending user data in Redis — NO DB write until OTP verified
        String encodedPassword = passwordEncoder.encode(request.getPassword());
        Map<String, String> pendingData = Map.of(
                "fullName", request.getFullName(),
                "email",    request.getEmail(),
                "password", encodedPassword,
                "phone",    request.getPhone() != null ? request.getPhone() : ""
        );

        String otp = OtpUtil.generateOtp();

        try {
            redisTemplate.opsForValue().set(
                    RedisKeyUtil.pendingUserKey(request.getEmail()),
                    objectMapper.writeValueAsString(pendingData),
                    OTP_TTL_SECONDS, TimeUnit.SECONDS
            );
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize pending user data", e);
        }

        redisTemplate.opsForValue().set(
                RedisKeyUtil.otpKey(OtpType.ACTIVATION, request.getEmail()),
                otp,
                OTP_TTL_SECONDS, TimeUnit.SECONDS
        );

        emailService.sendOtpEmail(request.getEmail(), otp, OtpType.ACTIVATION);
    }

    // ── Activate Account ──────────────────────────────────────────────────

    @Override
    @Transactional
    public void activateAccount(ActivateAccountRequest request) {
        String pendingJson = redisTemplate.opsForValue()
                .get(RedisKeyUtil.pendingUserKey(request.getEmail()));
        if (pendingJson == null) {
            throw new AppException(ErrorCode.PENDING_REGISTRATION_NOT_FOUND);
        }

        String storedOtp = redisTemplate.opsForValue()
                .get(RedisKeyUtil.otpKey(OtpType.ACTIVATION, request.getEmail()));
        if (storedOtp == null || !storedOtp.equals(request.getOtp())) {
            throw new AppException(ErrorCode.OTP_INVALID);
        }

        // Deserialize pending user data
        Map<?, ?> data;
        try {
            data = objectMapper.readValue(pendingJson, Map.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize pending user data", e);
        }

        RoleEntity defaultRole = roleRepository.findByRoleName("ROLE_USER")
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND));

        UserEntity user = UserEntity.builder()
                .fullName((String) data.get("fullName"))
                .email((String) data.get("email"))
                .password((String) data.get("password"))
                .phone((String) data.get("phone"))
                .isActive(true)
                .role(defaultRole)
                .build();

        userRepository.save(user);

        // Clean up Redis keys
        redisTemplate.delete(RedisKeyUtil.pendingUserKey(request.getEmail()));
        redisTemplate.delete(RedisKeyUtil.otpKey(OtpType.ACTIVATION, request.getEmail()));
    }

    // ── Login ─────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        UserEntity user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (Boolean.FALSE.equals(user.getIsActive())) {
            throw new AppException(ErrorCode.ACCOUNT_NOT_ACTIVE);
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new AppException(ErrorCode.INVALID_CREDENTIALS);
        }

        return generateAndSaveTokens(user, null);
    }

    // ── Logout ────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public void logout(LogoutRequest request) {
        try {
            String jti = jwtUtil.getJti(request.getAccessToken());
            long   ttl = jwtUtil.getRemainingTtlSeconds(request.getAccessToken());

            if (ttl > 0) {
                redisTemplate.opsForValue().set(
                        RedisKeyUtil.blacklistAtKey(jti),
                        "",
                        ttl, TimeUnit.SECONDS
                );
            }
        } catch (Exception ignored) {
            // AT already expired or invalid — still proceed to delete RT
        }

        tokenRepository.deleteByRefreshToken(request.getRefreshToken());
    }

    // ── Refresh ───────────────────────────────────────────────────────────

    @Override
    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request) {
        // Validate signature first
        SignedJWT parsed;
        try {
            parsed = jwtUtil.parseToken(request.getToken());
        } catch (Exception e) {
            throw new AppException(ErrorCode.REFRESH_TOKEN_INVALID);
        }

        // Find the stored RT row
        TokenEntity tokenEntity = tokenRepository.findByRefreshToken(request.getToken())
                .orElseThrow(() -> new AppException(ErrorCode.REFRESH_TOKEN_INVALID));

        // Check expiry
        if (tokenEntity.getExpiresAt() == null ||
                tokenEntity.getExpiresAt().isBefore(OffsetDateTime.now())) {
            tokenRepository.delete(tokenEntity);
            throw new AppException(ErrorCode.REFRESH_TOKEN_INVALID);
        }

        UserEntity user = tokenEntity.getUser();

        // Delete old RT row, generate + save new pair
        tokenRepository.delete(tokenEntity);
        return generateAndSaveTokens(user, tokenEntity.getDeviceInfo());
    }

    // ── Introspect ────────────────────────────────────────────────────────

    @Override
    public IntrospectResponse introspect(IntrospectRequest request) {
        try {
            SignedJWT parsed = jwtUtil.parseToken(request.getToken());
            String jti = parsed.getJWTClaimsSet().getJWTID();
            boolean blacklisted = Boolean.TRUE.equals(
                    redisTemplate.hasKey(RedisKeyUtil.blacklistAtKey(jti)));
            boolean valid = !blacklisted;
            return IntrospectResponse.builder().valid(valid).build();
        } catch (Exception e) {
            return IntrospectResponse.builder().valid(false).build();
        }
    }

    // ── Forgot Password ───────────────────────────────────────────────────

    @Override
    public void forgotPassword(ForgotPasswordRequest request, String clientIp) {
        userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        checkOtpRateLimit(OtpType.FORGOT_PASSWORD, request.getEmail());

        String otp = OtpUtil.generateOtp();

        redisTemplate.opsForValue().set(
                RedisKeyUtil.otpKey(OtpType.FORGOT_PASSWORD, request.getEmail()),
                otp,
                OTP_TTL_SECONDS, TimeUnit.SECONDS
        );
        redisTemplate.opsForValue().set(
                RedisKeyUtil.otpRateKey(OtpType.FORGOT_PASSWORD, request.getEmail()),
                "",
                OTP_RATE_TTL_SECONDS, TimeUnit.SECONDS
        );

        emailService.sendOtpEmail(request.getEmail(), otp, OtpType.FORGOT_PASSWORD);
    }

    // ── Reset Password ────────────────────────────────────────────────────

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        String storedOtp = redisTemplate.opsForValue()
                .get(RedisKeyUtil.otpKey(OtpType.FORGOT_PASSWORD, request.getEmail()));

        if (storedOtp == null || !storedOtp.equals(request.getOtp())) {
            throw new AppException(ErrorCode.OTP_INVALID);
        }

        UserEntity user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        redisTemplate.delete(RedisKeyUtil.otpKey(OtpType.FORGOT_PASSWORD, request.getEmail()));
    }

    // ── Resend OTP ────────────────────────────────────────────────────────

    @Override
    public void resendOtp(ResendOtpRequest request, String clientIp) {
        checkOtpRateLimit(request.getType(), request.getEmail());

        String otp = OtpUtil.generateOtp();

        redisTemplate.opsForValue().set(
                RedisKeyUtil.otpKey(request.getType(), request.getEmail()),
                otp,
                OTP_TTL_SECONDS, TimeUnit.SECONDS
        );
        redisTemplate.opsForValue().set(
                RedisKeyUtil.otpRateKey(request.getType(), request.getEmail()),
                "",
                OTP_RATE_TTL_SECONDS, TimeUnit.SECONDS
        );

        emailService.sendOtpEmail(request.getEmail(), otp, request.getType());
    }

    // ── Get Current User ──────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public UserResponse getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        return UserResponse.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .gender(user.getGender())
                .phone(user.getPhone())
                .dob(user.getDob())
                .isActive(user.getIsActive())
                .createdAt(user.getCreatedAt())
                .roleName(user.getRole() != null ? user.getRole().getRoleName() : null)
                .build();
    }

    // ── Private Helpers ───────────────────────────────────────────────────

    private AuthResponse generateAndSaveTokens(UserEntity user, String deviceInfo) {
        String accessToken  = jwtUtil.generateAccessToken(user);
        String refreshToken = jwtUtil.generateRefreshToken(user);

        TokenEntity tokenEntity = TokenEntity.builder()
                .refreshToken(refreshToken)
                .expiresAt(OffsetDateTime.now().plusDays(RT_TTL_DAYS))
                .deviceInfo(deviceInfo)
                .user(user)
                .build();

        tokenRepository.save(tokenEntity);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    private void checkOtpRateLimit(OtpType type, String email) {
        Boolean rateLimited = redisTemplate.hasKey(RedisKeyUtil.otpRateKey(type, email));
        if (Boolean.TRUE.equals(rateLimited)) {
            throw new AppException(ErrorCode.OTP_ALREADY_SENT);
        }
    }
}
