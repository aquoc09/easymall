package com.quocnva.easymall.service.auth;

import com.quocnva.easymall.dtos.request.auth.IntrospectRequest;
import com.quocnva.easymall.dtos.request.auth.RefreshTokenRequest;
import com.quocnva.easymall.dtos.response.auth.AuthResponse;
import com.quocnva.easymall.dtos.response.auth.IntrospectResponse;
import com.quocnva.easymall.entity.UserEntity;

/**
 * Token lifecycle management: generation, refresh rotation, and introspection.
 */
public interface TokenService {

    /**
     * Generates a new AT+RT pair, persists the RT row, and returns the response.
     *
     * @param user       the authenticated user
     * @param deviceInfo optional device identifier for audit
     * @return access + refresh token pair
     */
    AuthResponse generateAndSaveTokens(UserEntity user, String deviceInfo);

    /**
     * Rotates the RT — verifies expiry, deletes old row, issues new AT+RT.
     */
    AuthResponse refresh(RefreshTokenRequest request);

    /**
     * Returns whether the given AT is still valid (not expired, not blacklisted).
     */
    IntrospectResponse introspect(IntrospectRequest request);
}
