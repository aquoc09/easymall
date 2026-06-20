package com.quocnva.easymall.service.auth;

import com.quocnva.easymall.dtos.request.auth.ActivateAccountRequest;
import com.quocnva.easymall.dtos.request.auth.RegisterRequest;
import com.quocnva.easymall.dtos.request.auth.ResendOtpRequest;

/**
 * Handles the two-step registration flow (register → OTP verify → activate)
 * and OTP resending for both activation and password-reset purposes.
 */
public interface RegistrationService {

    void register(RegisterRequest request);

    void activateAccount(ActivateAccountRequest request);

    void resendOtp(ResendOtpRequest request, String clientIp);
}
