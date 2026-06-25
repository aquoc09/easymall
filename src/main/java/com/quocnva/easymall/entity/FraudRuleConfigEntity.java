package com.quocnva.easymall.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(name = "fraud_rule_configs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FraudRuleConfigEntity {

    @Id
    @Column(name = "config_id")
    private Integer configId;

    @Builder.Default
    @Column(name = "review_threshold", nullable = false, columnDefinition = "NUMERIC(5,2)")
    private Double reviewThreshold = 40.00;

    @Builder.Default
    @Column(name = "decline_threshold", nullable = false, columnDefinition = "NUMERIC(5,2)")
    private Double declineThreshold = 75.00;

    @Builder.Default
    @Column(name = "is_active")
    private Boolean isActive = true;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by")
    private UserEntity updatedBy;
}
