package com.quocnva.easymall.entity;

import com.quocnva.easymall.enums.SystemDecision;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(name = "fraud_records_and_labels")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FraudRecordEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "fraud_record_id")
    private Long fraudRecordId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private OrderEntity order;

    @Column(name = "risk_score", nullable = false, columnDefinition = "NUMERIC(5,2)")
    private Double riskScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "system_decision", nullable = false, length = 50)
    private SystemDecision systemDecision;

    @Builder.Default
    @Column(name = "final_label", nullable = false, length = 100)
    private String finalLabel = "PENDING";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "labeled_by")
    private UserEntity labeledBy;

    @Column(name = "analyst_notes", columnDefinition = "text")
    private String analystNotes;

    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @Column(name = "top_risk_factors", columnDefinition = "jsonb")
    private java.util.List<String> topRiskFactors;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
