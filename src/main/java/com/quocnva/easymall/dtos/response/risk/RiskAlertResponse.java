package com.quocnva.easymall.dtos.response.risk;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@Builder
public class RiskAlertResponse {
    private Long alertId;
    private Long userId;
    private Long orderId;
    private String ruleCode;
    private String description;
    private String status;
    private OffsetDateTime createdAt;
}
