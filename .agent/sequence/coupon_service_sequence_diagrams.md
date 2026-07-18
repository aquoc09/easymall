# Sequence Diagrams for Coupon Service

This document contains the sequence diagrams for all major operations within `CouponServiceImpl`.

## 1. Create Coupon (`createCoupon`)

```mermaid
sequenceDiagram
    participant Client
    participant CouponService
    participant CouponRepository
    participant CouponMapper

    Client->>CouponService: createCoupon(request)
    activate CouponService

    CouponService->>CouponService: normalize code

    CouponService->>CouponRepository: existsByCode(normalizedCode)
    activate CouponRepository
    alt Code exists
        CouponRepository-->>CouponService: true
        CouponService-->>Client: throw AppException(COUPON_CODE_ALREADY_EXISTS)
    else Code available
        CouponRepository-->>CouponService: false
    end
    deactivate CouponRepository

    alt DiscountType == PERCENTAGE and maxDiscountAmount == null
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

## 2. Update Coupon (`updateCoupon`)

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
    CouponRepository-->>CouponService: entity (or throw COUPON_NOT_FOUND)
    deactivate CouponRepository

    CouponService->>CouponService: update fields from request (excluding code)

    CouponService->>CouponRepository: save(entity)
    activate CouponRepository
    CouponRepository-->>CouponService: savedEntity
    deactivate CouponRepository

    CouponService->>CouponMapper: toResponse(savedEntity)
    CouponMapper-->>CouponService: CouponResponse

    CouponService-->>Client: CouponResponse
    deactivate CouponService
```

## 3. Preview Apply Coupon (`previewApply`)

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
    UserRepository-->>CouponService: UserEntity (or throw USER_NOT_FOUND)
    deactivate UserRepository

    %% Validate Coupon sub-process
    CouponService->>CouponService: validateCoupon(couponCode, orderAmount, user)
    activate CouponService
    CouponService->>CouponRepository: findByCode(couponCode)
    CouponRepository-->>CouponService: CouponEntity (or throw COUPON_NOT_FOUND)
    
    alt is not active
        CouponService-->>Client: throw AppException(COUPON_NOT_FOUND)
    end
    
    alt current time out of range
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

    %% Calculate Discount sub-process
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

## 4. Get Available Coupons (`getAvailableCoupons`)

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

    loop For each entity
        CouponService->>CouponMapper: toResponse(entity)
        CouponMapper-->>CouponService: CouponResponse
    end

    CouponService-->>Client: List<CouponResponse>
    deactivate CouponService
```
