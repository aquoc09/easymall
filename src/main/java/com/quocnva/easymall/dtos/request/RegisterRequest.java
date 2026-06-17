package com.quocnva.easymall.dtos.request;

import jakarta.validation.constraints.*;
import lombok.Getter;

@Getter
public class RegisterRequest {

    @NotBlank(message = "Full name must not be blank")
    private String fullName;

    @NotBlank(message = "Email must not be blank")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Password must not be blank")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    private String phone;
}
