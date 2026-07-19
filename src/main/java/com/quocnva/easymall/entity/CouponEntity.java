package com.quocnva.easymall.entity;

import com.quocnva.easymall.enums.CouponType;
import com.quocnva.easymall.enums.DiscountType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "coupons")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CouponEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "coupon_id")
    private Long couponId;

    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;

    @Column(name = "description", length = 255)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false, length = 20)
    private DiscountType discountType;

    @Column(name = "discount_value", nullable = false, precision = 15, scale = 2)
    private BigDecimal discountValue;

    /** Giới hạn giảm tối đa — bắt buộc khi discountType = PERCENTAGE */
    @Column(name = "max_discount_amount", precision = 15, scale = 2)
    private BigDecimal maxDiscountAmount;

    @Builder.Default
    @Column(name = "min_order_amount", precision = 15, scale = 2)
    private BigDecimal minOrderAmount = BigDecimal.ZERO;

    /** Tổng lượt dùng toàn sàn */
    @Builder.Default
    @Column(name = "max_usage")
    private Integer maxUsage = 1000;

    /** Số lượt mỗi user được dùng */
    @Builder.Default
    @Column(name = "user_usage_limit")
    private Integer userUsageLimit = 1;

    @Column(name = "start_date", nullable = false)
    private OffsetDateTime startDate;

    @Column(name = "end_date", nullable = false)
    private OffsetDateTime endDate;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    /**
     * applicable_conditions: JSONB — điều kiện áp dụng nâng cao.
     * Ví dụ: {"applicable_category_ids": [1, 2, 3]}
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "applicable_conditions", columnDefinition = "JSONB")
    private String applicableConditions;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "coupon_type", nullable = false, length = 30)
    private CouponType couponType = CouponType.SHOP_VOUCHER;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;
}
