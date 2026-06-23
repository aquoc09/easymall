package com.quocnva.easymall.service.coupon;

import com.quocnva.easymall.dtos.request.coupon.CouponApplyRequest;
import com.quocnva.easymall.dtos.request.coupon.CouponCreateRequest;
import com.quocnva.easymall.dtos.request.coupon.CouponUpdateRequest;
import com.quocnva.easymall.dtos.response.coupon.CouponApplyResponse;
import com.quocnva.easymall.dtos.response.coupon.CouponResponse;
import com.quocnva.easymall.entity.CouponEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CouponService {

    CouponResponse createCoupon(CouponCreateRequest request);

    CouponResponse updateCoupon(Long couponId, CouponUpdateRequest request);

    void deactivateCoupon(Long couponId);

    CouponResponse getCoupon(Long couponId);

    Page<CouponResponse> getAllCoupons(Pageable pageable);

    CouponApplyResponse previewApply(CouponApplyRequest request, String userEmail);

    /**
     * Validate coupon tại thời điểm checkout và trả về entity đã lock.
     * Internal — gọi từ OrderService trong cùng transaction.
     *
     * @param couponCode  mã coupon
     * @param orderAmount tổng tiền hàng
     * @param userEmail   email user đang checkout
     * @return CouponEntity hợp lệ
     */
    CouponEntity validateForCheckout(String couponCode, java.math.BigDecimal orderAmount, String userEmail);

    /**
     * TODO: Gợi ý coupon tốt nhất cho đơn hàng.
     * Stub — chưa implement, trả về empty list.
     */
    java.util.List<CouponApplyResponse> suggestCoupons(java.math.BigDecimal orderAmount, String userEmail);
}
