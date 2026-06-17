package com.quocnva.easymall.controller;

import com.quocnva.easymall.dtos.request.auth.*;
import com.quocnva.easymall.dtos.response.ApiResponse;
import com.quocnva.easymall.dtos.response.auth.AuthResponse;
import com.quocnva.easymall.dtos.response.auth.IntrospectResponse;
import com.quocnva.easymall.dtos.response.auth.UserResponse;
import com.quocnva.easymall.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * Step 1 of registration — stores pending user + sends OTP email.
     * No DB write occurs here.
     */
    @PostMapping("/register")
    public ApiResponse<Void> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return ApiResponse.<Void>builder()
                .message("Registration successful. Please check your email for the OTP.")
                .build();
    }

    /**
     * Step 2 of registration — verifies OTP and persists the user to DB.
     */
    @PostMapping("/active")
    public ApiResponse<Void> activateAccount(@Valid @RequestBody ActivateAccountRequest request) {
        authService.activateAccount(request);
        return ApiResponse.<Void>builder()
                .message("Account activated successfully.")
                .build();
    }

    /**
     * Resends OTP for ACTIVATION or FORGOT_PASSWORD flows.
     * Rate limited to one resend per 60 seconds per email+type.
     */
    @PostMapping("/resend-otp")
    public ApiResponse<Void> resendOtp(@Valid @RequestBody ResendOtpRequest request,
                                       HttpServletRequest httpRequest) {
        authService.resendOtp(request, httpRequest.getRemoteAddr());
        return ApiResponse.<Void>builder()
                .message("OTP resent successfully.")
                .build();
    }

    /**
     * Returns {accessToken, refreshToken, tokenType}.
     */
    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ApiResponse.<AuthResponse>builder()
                .result(response)
                .build();
    }

    /**
     * Blacklists the AT in Redis and deletes the RT row from DB.
     */
    @PostMapping("/logout")
    public ApiResponse<Void> logout(@Valid @RequestBody LogoutRequest request) {
        authService.logout(request);
        return ApiResponse.<Void>builder()
                .message("Logged out successfully.")
                .build();
    }

    /**
     * Rotates the RT — verifies expiry, deletes old row, issues new AT+RT.
     */
    @PostMapping("/refresh")
    public ApiResponse<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        AuthResponse response = authService.refresh(request);
        return ApiResponse.<AuthResponse>builder()
                .result(response)
                .build();
    }

    /**
     * Returns {valid: true/false} for the given AT.
     */
    @PostMapping("/introspect")
    public ApiResponse<IntrospectResponse> introspect(@Valid @RequestBody IntrospectRequest request) {
        IntrospectResponse response = authService.introspect(request);
        return ApiResponse.<IntrospectResponse>builder()
                .result(response)
                .build();
    }

    /**
     * Initiates password reset — sends OTP to the registered email.
     */
    @PostMapping("/forgot-password")
    public ApiResponse<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request,
                                            HttpServletRequest httpRequest) {
        authService.forgotPassword(request, httpRequest.getRemoteAddr());
        return ApiResponse.<Void>builder()
                .message("OTP sent to your email.")
                .build();
    }

    /**
     * Verifies OTP and updates the password.
     */
    @PostMapping("/reset-password")
    public ApiResponse<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ApiResponse.<Void>builder()
                .message("Password reset successfully.")
                .build();
    }

    /**
     * Returns the profile of the currently authenticated user.
     * Requires valid Bearer AT in Authorization header.
     */
    @GetMapping("/me")
    public ApiResponse<UserResponse> getCurrentUser() {
        UserResponse response = authService.getCurrentUser();
        return ApiResponse.<UserResponse>builder()
                .result(response)
                .build();
    }
}
