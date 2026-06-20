package com.quocnva.easymall.service.auth.impl;

import com.nimbusds.jwt.SignedJWT;
import com.quocnva.easymall.dtos.request.auth.IntrospectRequest;
import com.quocnva.easymall.dtos.request.auth.RefreshTokenRequest;
import com.quocnva.easymall.dtos.response.auth.AuthResponse;
import com.quocnva.easymall.dtos.response.auth.IntrospectResponse;
import com.quocnva.easymall.entity.TokenEntity;
import com.quocnva.easymall.entity.UserEntity;
import com.quocnva.easymall.exception.AppException;
import com.quocnva.easymall.exception.ErrorCode;
import com.quocnva.easymall.repository.TokenRepository;
import com.quocnva.easymall.service.auth.TokenService;
import com.quocnva.easymall.util.JwtUtil;
import com.quocnva.easymall.util.OtpConstants;
import com.quocnva.easymall.util.RedisKeyUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class TokenServiceImpl implements TokenService {

    private final JwtUtil jwtUtil;
    private final TokenRepository tokenRepository;
    private final RedisTemplate<String, String> redisTemplate;

    @Override
    @Transactional
    public AuthResponse generateAndSaveTokens(UserEntity user, String deviceInfo) {
        String accessToken = jwtUtil.generateAccessToken(user);
        String refreshToken = jwtUtil.generateRefreshToken(user);

        TokenEntity tokenEntity = TokenEntity.builder()
                .refreshToken(refreshToken)
                .expiresAt(OffsetDateTime.now().plusDays(OtpConstants.RT_TTL_DAYS))
                .deviceInfo(deviceInfo)
                .user(user)
                .build();

        tokenRepository.save(tokenEntity);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    @Override
    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request) {
        // Validate signature first
        try {
            jwtUtil.parseToken(request.getToken());
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
}
