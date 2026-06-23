package com.quocnva.easymall.dtos.response.coupon;

import com.quocnva.easymall.enums.CouponType;
import com.quocnva.easymall.enums.DiscountType;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Getter
@Setter
@Builder
public class CouponResponse {

    private Long couponId;
    private String code;
    private String description;
    private DiscountType discountType;
    private BigDecimal discountValue;
    private BigDecimal maxDiscountAmount;
    private BigDecimal minOrderAmount;
    private Integer maxUsage;
    private Integer userUsageLimit;
    private OffsetDateTime startDate;
    private OffsetDateTime endDate;
    private Boolean isActive;
    private String applicableConditions;
    private CouponType couponType;
    private OffsetDateTime createdAt;
}
