package com.quocnva.easymall.dtos.request.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class LogoutRequest {

    @NotBlank(message = "{validation.accessToken.not-blank}")
    private String accessToken;

    @NotBlank(message = "{validation.refreshToken.not-blank}")
    private String refreshToken;
}
