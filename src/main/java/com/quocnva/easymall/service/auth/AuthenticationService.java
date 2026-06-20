package com.quocnva.easymall.service.auth;

import com.quocnva.easymall.dtos.request.auth.*;
import com.quocnva.easymall.dtos.response.auth.AuthResponse;

/**
 * Handles credential-based authentication: login and logout.
 */
public interface AuthenticationService {

    AuthResponse login(LoginRequest request);

    void logout(LogoutRequest request);
}
