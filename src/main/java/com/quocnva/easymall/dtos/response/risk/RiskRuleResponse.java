package com.quocnva.easymall.dtos.response.risk;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
@Builder
public class RiskRuleResponse {
    private String ruleCode;
    private String ruleName;
    private String riskLevel;
    private BigDecimal thresholdValue;
    private Integer timeWindowMinutes;
    private Boolean isActive;
    private OffsetDateTime updatedAt;
}
