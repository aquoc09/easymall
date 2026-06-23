package com.quocnva.easymall.mapper;

import com.quocnva.easymall.dtos.request.coupon.CouponCreateRequest;
import com.quocnva.easymall.dtos.response.coupon.CouponApplyResponse;
import com.quocnva.easymall.dtos.response.coupon.CouponResponse;
import com.quocnva.easymall.entity.CouponEntity;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class CouponMapper {

    public CouponEntity toEntity(CouponCreateRequest request) {
        return CouponEntity.builder()
                .code(request.getCode().toUpperCase().trim())
                .description(request.getDescription())
                .discountType(request.getDiscountType())
                .discountValue(request.getDiscountValue())
                .maxDiscountAmount(request.getMaxDiscountAmount())
                .minOrderAmount(request.getMinOrderAmount() != null ? request.getMinOrderAmount() : BigDecimal.ZERO)
                .maxUsage(request.getMaxUsage() != null ? request.getMaxUsage() : 1000)
                .userUsageLimit(request.getUserUsageLimit() != null ? request.getUserUsageLimit() : 1)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .couponType(request.getCouponType())
                .applicableConditions(request.getApplicableConditions())
                .build();
    }

    public CouponResponse toResponse(CouponEntity entity) {
        return CouponResponse.builder()
                .couponId(entity.getCouponId())
                .code(entity.getCode())
                .description(entity.getDescription())
                .discountType(entity.getDiscountType())
                .discountValue(entity.getDiscountValue())
                .maxDiscountAmount(entity.getMaxDiscountAmount())
                .minOrderAmount(entity.getMinOrderAmount())
                .maxUsage(entity.getMaxUsage())
                .userUsageLimit(entity.getUserUsageLimit())
                .startDate(entity.getStartDate())
                .endDate(entity.getEndDate())
                .isActive(entity.getIsActive())
                .applicableConditions(entity.getApplicableConditions())
                .couponType(entity.getCouponType())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public CouponApplyResponse toApplyResponse(CouponEntity coupon,
                                               BigDecimal originalAmount,
                                               BigDecimal discountAmount) {
        return CouponApplyResponse.builder()
                .couponCode(coupon.getCode())
                .couponType(coupon.getCouponType())
                .originalOrderAmount(originalAmount)
                .discountAmount(discountAmount)
                .finalOrderAmount(originalAmount.subtract(discountAmount))
                .description(coupon.getDescription())
                .build();
    }
}
