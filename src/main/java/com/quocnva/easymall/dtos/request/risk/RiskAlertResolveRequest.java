package com.quocnva.easymall.dtos.request.risk;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RiskAlertResolveRequest {
    @NotBlank(message = "Status is required (RESOLVED or FALSE_POSITIVE)")
    private String status;
}
