package com.quocnva.easymall.dtos.response.auth;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class IntrospectResponse {

    private boolean valid;
}
