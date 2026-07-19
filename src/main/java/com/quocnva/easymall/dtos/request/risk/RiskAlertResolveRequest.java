package com.quocnva.easymall.dtos.request.risk;

import com.quocnva.easymall.enums.RiskAlertStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RiskAlertResolveRequest {
    @NotNull(message = "Status is required")
    private RiskAlertStatus status;
}
