package com.quocnva.easymall.dtos.request.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class LogoutRequest {

    @NotBlank(message = "Access token must not be blank")
    private String accessToken;

    @NotBlank(message = "Refresh token must not be blank")
    private String refreshToken;
}
