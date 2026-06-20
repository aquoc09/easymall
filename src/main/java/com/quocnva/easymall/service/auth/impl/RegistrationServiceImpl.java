package com.quocnva.easymall.service.auth.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quocnva.easymall.dtos.request.auth.ActivateAccountRequest;
import com.quocnva.easymall.dtos.request.auth.RegisterRequest;
import com.quocnva.easymall.dtos.request.auth.ResendOtpRequest;
import com.quocnva.easymall.entity.RoleEntity;
import com.quocnva.easymall.entity.UserEntity;
import com.quocnva.easymall.enums.OtpType;
import com.quocnva.easymall.exception.AppException;
import com.quocnva.easymall.exception.ErrorCode;
import com.quocnva.easymall.repository.RoleRepository;
import com.quocnva.easymall.repository.UserRepository;
import com.quocnva.easymall.service.email.EmailService;
import com.quocnva.easymall.service.auth.RegistrationService;
import com.quocnva.easymall.util.OtpConstants;
import com.quocnva.easymall.util.OtpUtil;
import com.quocnva.easymall.util.RedisKeyUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RegistrationServiceImpl implements RegistrationService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final EmailService emailService;

    // ── Register ─────────────────────────────────────────────────────────

    @Override
    public void register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AppException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        // Rate-limit: prevent OTP spam (1 per 60s per email)
        checkOtpRateLimit(OtpType.ACTIVATION, request.getEmail());

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
                    OtpConstants.OTP_TTL_SECONDS, TimeUnit.SECONDS
            );
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize pending user data", e);
        }

        redisTemplate.opsForValue().set(
                RedisKeyUtil.otpKey(OtpType.ACTIVATION, request.getEmail()),
                otp,
                OtpConstants.OTP_TTL_SECONDS, TimeUnit.SECONDS
        );

        // Set rate-limit sentinel
        redisTemplate.opsForValue().set(
                RedisKeyUtil.otpRateKey(OtpType.ACTIVATION, request.getEmail()),
                "",
                OtpConstants.OTP_RATE_TTL_SECONDS, TimeUnit.SECONDS
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

    // ── Resend OTP ────────────────────────────────────────────────────────

    @Override
    public void resendOtp(ResendOtpRequest request, String clientIp) {
        checkOtpRateLimit(request.getType(), request.getEmail());

        String otp = OtpUtil.generateOtp();

        redisTemplate.opsForValue().set(
                RedisKeyUtil.otpKey(request.getType(), request.getEmail()),
                otp,
                OtpConstants.OTP_TTL_SECONDS, TimeUnit.SECONDS
        );
        redisTemplate.opsForValue().set(
                RedisKeyUtil.otpRateKey(request.getType(), request.getEmail()),
                "",
                OtpConstants.OTP_RATE_TTL_SECONDS, TimeUnit.SECONDS
        );

        emailService.sendOtpEmail(request.getEmail(), otp, request.getType());
    }

    // ── Private Helpers ───────────────────────────────────────────────────

    private void checkOtpRateLimit(OtpType type, String email) {
        Boolean rateLimited = redisTemplate.hasKey(RedisKeyUtil.otpRateKey(type, email));
        if (Boolean.TRUE.equals(rateLimited)) {
            throw new AppException(ErrorCode.OTP_ALREADY_SENT);
        }
    }
}
