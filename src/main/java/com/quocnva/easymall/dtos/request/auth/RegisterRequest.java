package com.quocnva.easymall.dtos.request.auth;

import jakarta.validation.constraints.*;
import lombok.Getter;

@Getter
public class RegisterRequest {

    @NotBlank(message = "{validation.fullName.not-blank}")
    private String fullName;

    @NotBlank(message = "{validation.email.not-blank}")
    @Email(message = "{validation.email.invalid}")
    private String email;

    @NotBlank(message = "{validation.password.not-blank}")
    @Size(min = 8, message = "{validation.password.size}")
    private String password;

    private String phone;
}
