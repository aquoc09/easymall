package com.quocnva.easymall.service.auth.impl;

import com.quocnva.easymall.dtos.request.auth.LoginRequest;
import com.quocnva.easymall.dtos.request.auth.LogoutRequest;
import com.quocnva.easymall.dtos.response.auth.AuthResponse;
import com.quocnva.easymall.entity.UserEntity;
import com.quocnva.easymall.exception.AppException;
import com.quocnva.easymall.exception.ErrorCode;
import com.quocnva.easymall.repository.TokenRepository;
import com.quocnva.easymall.repository.UserRepository;
import com.quocnva.easymall.service.auth.AuthenticationService;
import com.quocnva.easymall.service.auth.TokenService;
import com.quocnva.easymall.util.JwtUtil;
import com.quocnva.easymall.util.RedisKeyUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class AuthenticationServiceImpl implements AuthenticationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final TokenRepository tokenRepository;
    private final TokenService tokenService;
    private final RedisTemplate<String, String> redisTemplate;

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

        return tokenService.generateAndSaveTokens(user, null);
    }

    // ── Logout ────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public void logout(LogoutRequest request) {
        try {
            String jti = jwtUtil.getJti(request.getAccessToken());
            long ttl = jwtUtil.getRemainingTtlSeconds(request.getAccessToken());

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
}
