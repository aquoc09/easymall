package com.quocnva.easymall.dtos.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class ActivateAccountRequest {

    @NotBlank(message = "Email must not be blank")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "OTP must not be blank")
    @Size(min = 6, max = 6, message = "OTP must be 6 digits")
    private String otp;
}
