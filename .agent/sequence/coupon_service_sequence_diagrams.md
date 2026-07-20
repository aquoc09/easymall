# Sequence Diagrams for Coupon Service

Tài liệu này chứa các sơ đồ tuần tự cho tất cả các hoạt động chính trong `CouponServiceImpl`.

## 1. Tạo Coupon (`createCoupon`)

```mermaid
sequenceDiagram
    participant Client
    participant CouponService
    participant CouponRepository
    participant CouponMapper

    Client->>CouponService: createCoupon(request)
    activate CouponService

    CouponService->>CouponService: chuẩn hóa code

    CouponService->>CouponRepository: existsByCode(normalizedCode)
    activate CouponRepository
    alt Code đã tồn tại
        CouponRepository-->>CouponService: true
        CouponService-->>Client: throw AppException(COUPON_CODE_ALREADY_EXISTS)
    else Code khả dụng
        CouponRepository-->>CouponService: false
    end
    deactivate CouponRepository

    alt DiscountType == PERCENTAGE và maxDiscountAmount == null
        CouponService-->>Client: throw AppException(BUDGET_EXCEEDED)
    end

    CouponService->>CouponMapper: toEntity(request)
    CouponMapper-->>CouponService: CouponEntity
    
    CouponService->>CouponService: entity.setCode(normalizedCode)

    CouponService->>CouponRepository: save(entity)
    activate CouponRepository
    CouponRepository-->>CouponService: savedEntity
    deactivate CouponRepository
    
    CouponService->>CouponMapper: toResponse(savedEntity)
    CouponMapper-->>CouponService: CouponResponse

    CouponService-->>Client: CouponResponse
    deactivate CouponService
```

## 2. Cập nhật Coupon (`updateCoupon`)

```mermaid
sequenceDiagram
    participant Client
    participant CouponService
    participant CouponRepository
    participant CouponMapper

    Client->>CouponService: updateCoupon(couponId, request)
    activate CouponService

    CouponService->>CouponRepository: findById(couponId)
    activate CouponRepository
    CouponRepository-->>CouponService: entity (hoặc ném ra COUPON_NOT_FOUND)
    deactivate CouponRepository

    CouponService->>CouponService: cập nhật các trường từ request (ngoại trừ code)

    CouponService->>CouponRepository: save(entity)
    activate CouponRepository
    CouponRepository-->>CouponService: savedEntity
    deactivate CouponRepository

    CouponService->>CouponMapper: toResponse(savedEntity)
    CouponMapper-->>CouponService: CouponResponse

    CouponService-->>Client: CouponResponse
    deactivate CouponService
```

## 3. Xem trước áp dụng Coupon (`previewApply`)

```mermaid
sequenceDiagram
    participant Client
    participant CouponService
    participant UserRepository
    participant CouponRepository
    participant CouponUsageRepository
    participant CouponMapper

    Client->>CouponService: previewApply(request, userEmail)
    activate CouponService

    CouponService->>UserRepository: findByEmail(userEmail)
    activate UserRepository
    UserRepository-->>CouponService: UserEntity (hoặc ném ra USER_NOT_FOUND)
    deactivate UserRepository

    %% Tiến trình con xác thực Coupon
    CouponService->>CouponService: validateCoupon(couponCode, orderAmount, user)
    activate CouponService
    CouponService->>CouponRepository: findByCode(couponCode)
    CouponRepository-->>CouponService: CouponEntity (hoặc ném ra COUPON_NOT_FOUND)
    
    alt không hoạt động
        CouponService-->>Client: throw AppException(COUPON_NOT_FOUND)
    end
    
    alt thời gian hiện tại ngoài phạm vi
        CouponService-->>Client: throw AppException(COUPON_EXPIRED)
    end

    CouponService->>CouponUsageRepository: countByCoupon(coupon)
    CouponUsageRepository-->>CouponService: totalUsed
    alt totalUsed >= maxUsage
        CouponService-->>Client: throw AppException(COUPON_EXHAUSTED)
    end

    CouponService->>CouponUsageRepository: countByCouponAndUser(coupon, user)
    CouponUsageRepository-->>CouponService: userUsed
    alt userUsed >= userUsageLimit
        CouponService-->>Client: throw AppException(COUPON_USAGE_LIMIT_EXCEEDED)
    end

    alt orderAmount < minOrderAmount
        CouponService-->>Client: throw AppException(INADEQUATE_ORDER_VALUE)
    end
    deactivate CouponService

    %% Tiến trình con tính toán giảm giá
    CouponService->>CouponService: calculateDiscount(coupon, orderAmount)
    activate CouponService
    alt type == PERCENTAGE
        CouponService-->>CouponService: min((amount * value / 100), maxDiscountAmount)
    else type == FIXED_AMOUNT
        CouponService-->>CouponService: min(value, orderAmount)
    end
    deactivate CouponService

    CouponService->>CouponMapper: toApplyResponse(coupon, orderAmount, discount)
    CouponMapper-->>CouponService: CouponApplyResponse

    CouponService-->>Client: CouponApplyResponse
    deactivate CouponService
```

## 4. Lấy các Coupon khả dụng (`getAvailableCoupons`)

```mermaid
sequenceDiagram
    participant Client
    participant CouponService
    participant CouponRepository
    participant CouponMapper

    Client->>CouponService: getAvailableCoupons()
    activate CouponService

    CouponService->>CouponRepository: findAvailableCoupons(now)
    activate CouponRepository
    CouponRepository-->>CouponService: List<CouponEntity>
    deactivate CouponRepository

    loop Đối với mỗi entity
        CouponService->>CouponMapper: toResponse(entity)
        CouponMapper-->>CouponService: CouponResponse
    end

    CouponService-->>Client: List<CouponResponse>
    deactivate CouponService
```
