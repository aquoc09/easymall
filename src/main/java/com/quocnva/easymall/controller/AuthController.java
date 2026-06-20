package com.quocnva.easymall.controller;

import com.quocnva.easymall.dtos.request.auth.*;
import com.quocnva.easymall.dtos.response.ApiResponse;
import com.quocnva.easymall.dtos.response.auth.AuthResponse;
import com.quocnva.easymall.dtos.response.auth.IntrospectResponse;
import com.quocnva.easymall.dtos.response.auth.UserResponse;
import com.quocnva.easymall.service.user.UserService;
import com.quocnva.easymall.service.auth.AuthenticationService;
import com.quocnva.easymall.service.auth.PasswordResetService;
import com.quocnva.easymall.service.auth.RegistrationService;
import com.quocnva.easymall.service.auth.TokenService;
import com.quocnva.easymall.util.Translator;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final RegistrationService registrationService;
    private final AuthenticationService authenticationService;
    private final TokenService tokenService;
    private final PasswordResetService passwordResetService;
    private final UserService userService;

    /**
     * Step 1 of registration — stores pending user + sends OTP email.
     * Rate limited to one per 60 seconds per email.
     * No DB write occurs here.
     */
    @PostMapping("/register")
    public ApiResponse<Void> register(@Valid @RequestBody RegisterRequest request) {
        registrationService.register(request);
        return ApiResponse.<Void>builder()
                .message(Translator.toLocale("success.register"))
                .build();
    }

    /**
     * Step 2 of registration — verifies OTP and persists the user to DB.
     */
    @PostMapping("/active")
    public ApiResponse<Void> activateAccount(@Valid @RequestBody ActivateAccountRequest request) {
        registrationService.activateAccount(request);
        return ApiResponse.<Void>builder()
                .message(Translator.toLocale("success.activate-account"))
                .build();
    }

    /**
     * Resends OTP for ACTIVATION or FORGOT_PASSWORD flows.
     * Rate limited to one resend per 60 seconds per email+type.
     */
    @PostMapping("/resend-otp")
    public ApiResponse<Void> resendOtp(@Valid @RequestBody ResendOtpRequest request,
            HttpServletRequest httpRequest) {
        registrationService.resendOtp(request, httpRequest.getRemoteAddr());
        return ApiResponse.<Void>builder()
                .message(Translator.toLocale("success.resend-otp"))
                .build();
    }

    /**
     * Returns {accessToken, refreshToken, tokenType}.
     */
    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authenticationService.login(request);
        return ApiResponse.<AuthResponse>builder()
                .result(response)
                .build();
    }

    /**
     * Blacklists the AT in Redis and deletes the RT row from DB.
     */
    @PostMapping("/logout")
    public ApiResponse<Void> logout(@Valid @RequestBody LogoutRequest request) {
        authenticationService.logout(request);
        return ApiResponse.<Void>builder()
                .message(Translator.toLocale("success.logout"))
                .build();
    }

    /**
     * Rotates the RT — verifies expiry, deletes old row, issues new AT+RT.
     */
    @PostMapping("/refresh")
    public ApiResponse<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        AuthResponse response = tokenService.refresh(request);
        return ApiResponse.<AuthResponse>builder()
                .result(response)
                .build();
    }

    /**
     * Returns {valid: true/false} for the given AT.
     */
    @PostMapping("/introspect")
    public ApiResponse<IntrospectResponse> introspect(@Valid @RequestBody IntrospectRequest request) {
        IntrospectResponse response = tokenService.introspect(request);
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
        passwordResetService.forgotPassword(request, httpRequest.getRemoteAddr());
        return ApiResponse.<Void>builder()
                .message(Translator.toLocale("success.forgot-password"))
                .build();
    }

    /**
     * Verifies OTP and updates the password.
     */
    @PostMapping("/reset-password")
    public ApiResponse<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.resetPassword(request);
        return ApiResponse.<Void>builder()
                .message(Translator.toLocale("success.reset-password"))
                .build();
    }

    /**
     * Returns the profile of the currently authenticated user.
     * Requires valid Bearer AT in Authorization header.
     */
    @GetMapping("/me")
    public ApiResponse<UserResponse> getCurrentUser() {
        UserResponse response = userService.getCurrentUser();
        return ApiResponse.<UserResponse>builder()
                .result(response)
                .build();
    }
}
