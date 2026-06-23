package com.quocnva.easymall.dtos.request.coupon;

import com.quocnva.easymall.enums.CouponType;
import com.quocnva.easymall.enums.DiscountType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Getter
@Setter
public class CouponCreateRequest {

    @NotBlank(message = "{validation.couponCode.not-blank}")
    private String code;

    private String description;

    @NotNull(message = "{validation.discountType.not-null}")
    private DiscountType discountType;

    @NotNull(message = "{validation.discountValue.not-null}")
    @DecimalMin(value = "0.01", message = "{validation.discountValue.min}")
    private BigDecimal discountValue;

    /** Bắt buộc khi discountType = PERCENTAGE */
    private BigDecimal maxDiscountAmount;

    @DecimalMin(value = "0", message = "{validation.orderAmount.min}")
    private BigDecimal minOrderAmount;

    private Integer maxUsage;

    private Integer userUsageLimit;

    @NotNull(message = "{validation.startDate.not-null}")
    private OffsetDateTime startDate;

    @NotNull(message = "{validation.endDate.not-null}")
    private OffsetDateTime endDate;

    @NotNull(message = "{validation.couponType.not-null}")
    private CouponType couponType;

    private String applicableConditions;
}
