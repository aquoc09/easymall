package com.quocnva.easymall.dtos.request.order;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderCancelRequest {

    @NotBlank(message = "{validation.cancelReason.not-blank}")
    private String reason;
}
