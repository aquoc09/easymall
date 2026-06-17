package com.quocnva.easymall.dtos.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AuthResponse {

    private String token;
    private boolean authenticated;
}
