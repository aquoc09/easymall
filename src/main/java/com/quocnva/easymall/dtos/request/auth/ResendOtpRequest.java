package com.quocnva.easymall.dtos.request.auth;

import com.quocnva.easymall.enums.OtpType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class ResendOtpRequest {

    @NotBlank(message = "{validation.email.not-blank}")
    @Email(message = "{validation.email.invalid}")
    private String email;

    @NotNull(message = "{validation.otp-type.not-null}")
    private OtpType type;
}
