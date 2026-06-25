package com.quocnva.easymall.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(name = "user_stats")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserStatsEntity {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private UserEntity user;

    @Builder.Default
    @Column(name = "total_orders")
    private Integer totalOrders = 0;

    @Builder.Default
    @Column(name = "returned_orders_count")
    private Integer returnedOrdersCount = 0;

    @Builder.Default
    @Column(name = "reputation_score", columnDefinition = "NUMERIC(5,2)")
    private Double reputationScore = 100.00;

    @Builder.Default
    @Column(name = "is_restricted")
    private Boolean isRestricted = false;

    @Builder.Default
    @Column(name = "account_age_days")
    private Integer accountAgeDays = 0;

    @Builder.Default
    @Column(name = "failed_payment_attempts_10m")
    private Integer failedPaymentAttempts10m = 0;

    @Builder.Default
    @Column(name = "total_distinct_devices")
    private Integer totalDistinctDevices = 1;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
