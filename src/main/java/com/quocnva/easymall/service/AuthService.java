package com.quocnva.easymall.service;

import com.quocnva.easymall.dtos.request.auth.*;
import com.quocnva.easymall.dtos.response.auth.AuthResponse;
import com.quocnva.easymall.dtos.response.auth.IntrospectResponse;
import com.quocnva.easymall.dtos.response.auth.UserResponse;

public interface AuthService {

    void register(RegisterRequest request);

    void activateAccount(ActivateAccountRequest request);

    void resendOtp(ResendOtpRequest request, String clientIp);

    AuthResponse login(LoginRequest request);

    void logout(LogoutRequest request);

    AuthResponse refresh(RefreshTokenRequest request);

    IntrospectResponse introspect(IntrospectRequest request);

    void forgotPassword(ForgotPasswordRequest request, String clientIp);

    void resetPassword(ResetPasswordRequest request);

    UserResponse getCurrentUser();
}
