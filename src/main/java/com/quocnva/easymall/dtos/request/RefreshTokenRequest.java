package com.quocnva.easymall.dtos.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class RefreshTokenRequest {

    @NotBlank(message = "Token must not be blank")
    private String token;
}
