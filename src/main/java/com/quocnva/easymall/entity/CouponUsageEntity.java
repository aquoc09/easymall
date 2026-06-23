package com.quocnva.easymall.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "coupon_usages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CouponUsageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "coupon_usage_id")
    private Long couponUsageId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coupon_id")
    private CouponEntity coupon;

    /**
     * orderId lưu thô (không @ManyToOne) để tránh circular dependency với OrderEntity.
     * Việc xóa theo orderId dùng @Modifying query.
     */
    @Column(name = "order_id")
    private Long orderId;

    @Builder.Default
    @Column(name = "used_at", nullable = false)
    private OffsetDateTime usedAt = OffsetDateTime.now();
}
