package com.quocnva.easymall.dtos.request.coupon;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Getter
@Setter
public class CouponUpdateRequest {

    private String description;

    private BigDecimal discountValue;

    private BigDecimal maxDiscountAmount;

    private BigDecimal minOrderAmount;

    private Integer maxUsage;

    private Integer userUsageLimit;

    private OffsetDateTime startDate;

    private OffsetDateTime endDate;

    private Boolean isActive;

    private String applicableConditions;
}
