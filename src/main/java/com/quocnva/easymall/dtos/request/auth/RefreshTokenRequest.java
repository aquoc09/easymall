package com.quocnva.easymall.dtos.request.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class RefreshTokenRequest {

    @NotBlank(message = "{validation.token.not-blank}")
    private String token;
}
