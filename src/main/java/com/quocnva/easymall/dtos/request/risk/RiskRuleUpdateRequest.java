package com.quocnva.easymall.dtos.request.risk;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class RiskRuleUpdateRequest {
    @NotNull(message = "Threshold value is required")
    private BigDecimal thresholdValue;

    private Integer timeWindowMinutes;

    @NotNull(message = "Is Active status is required")
    private Boolean isActive;
}
