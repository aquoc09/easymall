package com.quocnva.easymall.service.coupon.impl;

import com.quocnva.easymall.dtos.request.coupon.CouponApplyRequest;
import com.quocnva.easymall.dtos.request.coupon.CouponCreateRequest;
import com.quocnva.easymall.dtos.request.coupon.CouponUpdateRequest;
import com.quocnva.easymall.dtos.response.coupon.CouponApplyResponse;
import com.quocnva.easymall.dtos.response.coupon.CouponResponse;
import com.quocnva.easymall.entity.CouponEntity;
import com.quocnva.easymall.entity.UserEntity;
import com.quocnva.easymall.enums.DiscountType;
import com.quocnva.easymall.exception.AppException;
import com.quocnva.easymall.exception.ErrorCode;
import com.quocnva.easymall.mapper.CouponMapper;
import com.quocnva.easymall.repository.CouponRepository;
import com.quocnva.easymall.repository.CouponUsageRepository;
import com.quocnva.easymall.repository.UserRepository;
import com.quocnva.easymall.service.coupon.CouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CouponServiceImpl implements CouponService {

    private final CouponRepository couponRepository;
    private final CouponUsageRepository couponUsageRepository;
    private final UserRepository userRepository;
    private final CouponMapper couponMapper;

    // ── ADMIN CRUD ────────────────────────────────────────────────────────

    @Override
    @Transactional
    public CouponResponse createCoupon(CouponCreateRequest request) {
        String normalizedCode = request.getCode().toUpperCase().trim();

        // 1. Kiểm tra unique code
        if (couponRepository.existsByCode(normalizedCode)) {
            throw new AppException(ErrorCode.COUPON_CODE_ALREADY_EXISTS);
        }

        // 2. PERCENTAGE phải có maxDiscountAmount
        if (request.getDiscountType() == DiscountType.PERCENTAGE
                && request.getMaxDiscountAmount() == null) {
            throw new AppException(ErrorCode.BUDGET_EXCEEDED);
        }

        CouponEntity entity = couponMapper.toEntity(request);
        entity.setCode(normalizedCode);

        return couponMapper.toResponse(couponRepository.save(entity));
    }

    @Override
    @Transactional
    public CouponResponse updateCoupon(Long couponId, CouponUpdateRequest request) {
        CouponEntity entity = getCouponEntityById(couponId);

        // code là immutable — không cho sửa
        if (request.getDescription() != null)
            entity.setDescription(request.getDescription());
        if (request.getDiscountValue() != null)
            entity.setDiscountValue(request.getDiscountValue());
        if (request.getMaxDiscountAmount() != null)
            entity.setMaxDiscountAmount(request.getMaxDiscountAmount());
        if (request.getMinOrderAmount() != null)
            entity.setMinOrderAmount(request.getMinOrderAmount());
        if (request.getMaxUsage() != null)
            entity.setMaxUsage(request.getMaxUsage());
        if (request.getUserUsageLimit() != null)
            entity.setUserUsageLimit(request.getUserUsageLimit());
        if (request.getStartDate() != null)
            entity.setStartDate(request.getStartDate());
        if (request.getEndDate() != null)
            entity.setEndDate(request.getEndDate());
        if (request.getIsActive() != null)
            entity.setIsActive(request.getIsActive());
        if (request.getApplicableConditions() != null)
            entity.setApplicableConditions(request.getApplicableConditions());

        return couponMapper.toResponse(couponRepository.save(entity));
    }

    @Override
    @Transactional
    public void deactivateCoupon(Long couponId) {
        CouponEntity entity = getCouponEntityById(couponId);
        entity.setIsActive(false);
        couponRepository.save(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public CouponResponse getCoupon(Long couponId) {
        return couponMapper.toResponse(getCouponEntityById(couponId));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CouponResponse> getAllCoupons(Pageable pageable) {
        return couponRepository.findAllByOrderByCouponIdDesc(pageable)
                .map(couponMapper::toResponse);
    }

    // ── USER PREVIEW ──────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public CouponApplyResponse previewApply(CouponApplyRequest request, String userEmail) {
        UserEntity user = getUserByEmail(userEmail);
        CouponEntity coupon = validateCoupon(request.getCouponCode(), request.getOrderAmount(), user);
        BigDecimal discount = calculateDiscount(coupon, request.getOrderAmount(), request.getShippingFee());
        return couponMapper.toApplyResponse(coupon, request.getOrderAmount(), discount);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CouponResponse> getAvailableCoupons(String userEmail) {
        OffsetDateTime now = OffsetDateTime.now();
        List<CouponEntity> availableCoupons = couponRepository.findAvailableCoupons(now);
        
        if (userEmail != null && !userEmail.equals("anonymousUser")) {
            UserEntity user = getUserByEmail(userEmail);
            return availableCoupons.stream()
                    .filter(coupon -> {
                        long userUsed = couponUsageRepository.countByCouponAndUser(coupon, user);
                        return userUsed < coupon.getUserUsageLimit();
                    })
                    .map(couponMapper::toResponse)
                    .toList();
        }
        
        return availableCoupons.stream()
                .map(couponMapper::toResponse)
                .toList();
    }

    // ── INTERNAL — gọi từ OrderService trong cùng @Transactional ──────────

    @Override
    @Transactional(readOnly = true)
    public CouponEntity validateForCheckout(String couponCode, BigDecimal orderAmount, String userEmail, com.quocnva.easymall.enums.PaymentMethod paymentMethod) {
        UserEntity user = getUserByEmail(userEmail);
        CouponEntity coupon = validateCoupon(couponCode, orderAmount, user);
        
        if (coupon.getCouponType() == com.quocnva.easymall.enums.CouponType.PAYMENT_VOUCHER && paymentMethod != null) {
            String conditions = coupon.getApplicableConditions();
            if (conditions != null && !conditions.trim().isEmpty()) {
                try {
                    com.fasterxml.jackson.databind.JsonNode root = new com.fasterxml.jackson.databind.ObjectMapper().readTree(conditions);
                    if (root.has("payment_method")) {
                        String requiredMethod = root.get("payment_method").asText();
                        if (!requiredMethod.equalsIgnoreCase(paymentMethod.name())) {
                            throw new AppException(ErrorCode.INVALID_PAYMENT_METHOD_FOR_COUPON);
                        }
                    }
                } catch (Exception e) {
                    // Ignore parse errors or handle them
                }
            }
        }
        
        return coupon;
    }

    // ── TODO STUB ─────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<CouponApplyResponse> suggestCoupons(BigDecimal orderAmount, String userEmail) {
        // TODO: implement coupon suggestion engine
        return Collections.emptyList();
    }

    // ── Private helpers ───────────────────────────────────────────────────

    private CouponEntity getCouponEntityById(Long couponId) {
        return couponRepository.findById(couponId)
                .orElseThrow(() -> new AppException(ErrorCode.COUPON_NOT_FOUND));
    }

    private UserEntity getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }

    /**
     * 5-step validate:
     * 1) tồn tại + active
     * 2) thời gian
     * 3) quota tổng
     * 4) per-user limit
     * 5) min_order_amount
     */
    private CouponEntity validateCoupon(String couponCode, BigDecimal orderAmount, UserEntity user) {
        // Step 1: tồn tại + active
        CouponEntity coupon = couponRepository.findByCode(couponCode.toUpperCase().trim())
                .orElseThrow(() -> new AppException(ErrorCode.COUPON_NOT_FOUND));
        if (!Boolean.TRUE.equals(coupon.getIsActive())) {
            throw new AppException(ErrorCode.COUPON_NOT_FOUND);
        }

        // Step 2: thời gian
        OffsetDateTime now = OffsetDateTime.now();
        if (now.isBefore(coupon.getStartDate()) || now.isAfter(coupon.getEndDate())) {
            throw new AppException(ErrorCode.COUPON_EXPIRED);
        }

        // Step 3: quota tổng
        long totalUsed = couponUsageRepository.countByCoupon(coupon);
        if (totalUsed >= coupon.getMaxUsage()) {
            throw new AppException(ErrorCode.COUPON_EXHAUSTED);
        }

        // Step 4: per-user limit
        long userUsed = couponUsageRepository.countByCouponAndUser(coupon, user);
        if (userUsed >= coupon.getUserUsageLimit()) {
            throw new AppException(ErrorCode.COUPON_USAGE_LIMIT_EXCEEDED);
        }

        // Step 5: min_order_amount
        if (orderAmount.compareTo(coupon.getMinOrderAmount()) < 0) {
            throw new AppException(ErrorCode.INADEQUATE_ORDER_VALUE);
        }

        return coupon;
    }

    /**
     * Tính discount:
     * - PERCENTAGE: min(amount * percent/100, maxDiscountAmount)
     * - FIXED_AMOUNT: discountValue (không vượt quá orderAmount)
     */
    public BigDecimal calculateDiscount(CouponEntity coupon, BigDecimal orderAmount, BigDecimal shippingFee) {
        BigDecimal baseAmount = (coupon.getCouponType() == com.quocnva.easymall.enums.CouponType.FREE_SHIPPING) ? shippingFee : orderAmount;

        if (coupon.getDiscountType() == DiscountType.PERCENTAGE) {
            BigDecimal raw = baseAmount
                    .multiply(coupon.getDiscountValue())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            return coupon.getMaxDiscountAmount() != null
                    ? raw.min(coupon.getMaxDiscountAmount())
                    : raw;
        }
        // FIXED_AMOUNT — không giảm quá baseAmount
        return coupon.getDiscountValue().min(baseAmount);
    }
}
