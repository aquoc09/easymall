package com.quocnva.easymall.dtos.request.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class ResetPasswordRequest {

    @NotBlank(message = "{validation.email.not-blank}")
    @Email(message = "{validation.email.invalid}")
    private String email;

    @NotBlank(message = "{validation.otp.not-blank}")
    @Size(min = 6, max = 6, message = "{validation.otp.size}")
    private String otp;

    @NotBlank(message = "{validation.newPassword.not-blank}")
    @Size(min = 8, message = "{validation.password.size}")
    private String newPassword;
}
