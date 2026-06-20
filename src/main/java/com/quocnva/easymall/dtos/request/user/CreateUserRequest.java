package com.quocnva.easymall.dtos.request.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateUserRequest {

    @NotBlank(message = "{validation.fullName.not-blank}")
    private String fullName;

    @NotBlank(message = "{validation.email.not-blank}")
    @Email(message = "{validation.email.invalid}")
    private String email;

    @NotBlank(message = "{validation.password.not-blank}")
    @Size(min = 8, message = "{validation.password.size}")
    private String password;

    private String phone;

    @NotNull
    private Long roleId;
}
