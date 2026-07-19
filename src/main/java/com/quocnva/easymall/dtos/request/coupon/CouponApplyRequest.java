package com.quocnva.easymall.dtos.request.coupon;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class CouponApplyRequest {

    @NotBlank(message = "{validation.couponCode.not-blank}")
    private String couponCode;

    @NotNull(message = "{validation.orderAmount.not-null}")
    @DecimalMin(value = "0", message = "{validation.orderAmount.min}")
    private BigDecimal orderAmount;

    // Optional: for accurately previewing FREE_SHIPPING coupons
    @DecimalMin(value = "0", message = "{validation.shippingFee.min}")
    private BigDecimal shippingFee = BigDecimal.ZERO;
}
