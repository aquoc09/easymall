package com.quocnva.easymall.dtos.response.coupon;

import com.quocnva.easymall.enums.CouponType;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
public class CouponApplyResponse {

    private String couponCode;
    private CouponType couponType;
    private BigDecimal originalOrderAmount;
    private BigDecimal discountAmount;
    private BigDecimal finalOrderAmount;
    private String description;
}
