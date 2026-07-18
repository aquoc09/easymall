package com.quocnva.easymall.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "risk_rule_configs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RiskRuleConfigEntity {

    @Id
    @Column(name = "rule_code", length = 50, nullable = false)
    private String ruleCode;

    @Column(name = "rule_name", length = 255, nullable = false)
    private String ruleName;

    @Column(name = "risk_level", length = 20, nullable = false)
    private String riskLevel;

    @Column(name = "threshold_value", precision = 15, scale = 2, nullable = false)
    private BigDecimal thresholdValue;

    @Column(name = "time_window_minutes")
    private Integer timeWindowMinutes;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "updated_at")
    @UpdateTimestamp
    private OffsetDateTime updatedAt;
}
