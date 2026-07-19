package com.quocnva.easymall.controller;

import com.quocnva.easymall.util.Translator;

import com.quocnva.easymall.dtos.request.coupon.CouponApplyRequest;
import com.quocnva.easymall.dtos.request.coupon.CouponCreateRequest;
import com.quocnva.easymall.dtos.request.coupon.CouponUpdateRequest;
import com.quocnva.easymall.dtos.response.ApiResponse;
import com.quocnva.easymall.dtos.response.coupon.CouponApplyResponse;
import com.quocnva.easymall.dtos.response.coupon.CouponResponse;
import com.quocnva.easymall.service.coupon.CouponService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    // ── ADMIN CRUD ─────────────────────────────────────────────────────────

    @PostMapping
    @PreAuthorize("@permissionChecker.has('coupon:manage')")
    public ApiResponse<CouponResponse> createCoupon(
            @Valid @RequestBody CouponCreateRequest request) {
        return ApiResponse.<CouponResponse>builder()
                .result(couponService.createCoupon(request))
                .message(Translator.toLocale("success.coupon.created"))
                .build();
    }

    @PutMapping("/{id}")
    @PreAuthorize("@permissionChecker.has('coupon:manage')")
    public ApiResponse<CouponResponse> updateCoupon(
            @PathVariable Long id,
            @Valid @RequestBody CouponUpdateRequest request) {
        return ApiResponse.<CouponResponse>builder()
                .result(couponService.updateCoupon(id, request))
                .message(Translator.toLocale("success.coupon.updated"))
                .build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@permissionChecker.has('coupon:manage')")
    public ApiResponse<Void> deactivateCoupon(@PathVariable Long id) {
        couponService.deactivateCoupon(id);
        return ApiResponse.<Void>builder()
                .message(Translator.toLocale("success.coupon.deactivated"))
                .build();
    }

    @GetMapping
    @PreAuthorize("@permissionChecker.has('coupon:manage')")
    public ApiResponse<Page<CouponResponse>> getAllCoupons(
            @PageableDefault(size = 20, sort = "couponId") Pageable pageable) {
        return ApiResponse.<Page<CouponResponse>>builder()
                .result(couponService.getAllCoupons(pageable))
                .build();
    }

    @GetMapping("/{id}")
    @PreAuthorize("@permissionChecker.has('coupon:manage')")
    public ApiResponse<CouponResponse> getCoupon(@PathVariable Long id) {
        return ApiResponse.<CouponResponse>builder()
                .result(couponService.getCoupon(id))
                .build();
    }

    // ── USER ───────────────────────────────────────────────────────

    @PostMapping("/preview")
    @PreAuthorize("@permissionChecker.has('coupon:apply')")
    public ApiResponse<CouponApplyResponse> previewApply(
            @Valid @RequestBody CouponApplyRequest request,
            Authentication authentication) {
        return ApiResponse.<CouponApplyResponse>builder()
                .result(couponService.previewApply(request, authentication.getName()))
                .build();
    }

    @GetMapping("/available")
    public ApiResponse<java.util.List<CouponResponse>> getAvailableCoupons() {
        return ApiResponse.<java.util.List<CouponResponse>>builder()
                .result(couponService.getAvailableCoupons())
                .message(Translator.toLocale("success.coupon.fetched"))
                .build();
    }
}
