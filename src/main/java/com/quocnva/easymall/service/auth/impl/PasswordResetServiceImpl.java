package com.quocnva.easymall.service.auth.impl;

import com.quocnva.easymall.dtos.request.auth.ForgotPasswordRequest;
import com.quocnva.easymall.dtos.request.auth.ResetPasswordRequest;
import com.quocnva.easymall.entity.UserEntity;
import com.quocnva.easymall.enums.OtpType;
import com.quocnva.easymall.exception.AppException;
import com.quocnva.easymall.exception.ErrorCode;
import com.quocnva.easymall.repository.UserRepository;
import com.quocnva.easymall.service.email.EmailService;
import com.quocnva.easymall.service.auth.PasswordResetService;
import com.quocnva.easymall.util.OtpConstants;
import com.quocnva.easymall.util.OtpUtil;
import com.quocnva.easymall.util.RedisKeyUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class PasswordResetServiceImpl implements PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RedisTemplate<String, String> redisTemplate;
    private final EmailService emailService;

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
                OtpConstants.OTP_TTL_SECONDS, TimeUnit.SECONDS
        );
        redisTemplate.opsForValue().set(
                RedisKeyUtil.otpRateKey(OtpType.FORGOT_PASSWORD, request.getEmail()),
                "",
                OtpConstants.OTP_RATE_TTL_SECONDS, TimeUnit.SECONDS
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

    // ── Private Helpers ───────────────────────────────────────────────────

    private void checkOtpRateLimit(OtpType type, String email) {
        Boolean rateLimited = redisTemplate.hasKey(RedisKeyUtil.otpRateKey(type, email));
        if (Boolean.TRUE.equals(rateLimited)) {
            throw new AppException(ErrorCode.OTP_ALREADY_SENT);
        }
    }
}
