# Sequence Diagrams for Order Service

This document contains the sequence diagrams for all operations within `OrderServiceImpl`.

## 1. Checkout (`checkout`)

```mermaid
sequenceDiagram
    participant Client
    participant OrderService
    participant AddressRepository
    participant CartItemRepository
    participant ProductVariantRepository
    participant CouponService
    participant GhnShippingService
    participant OrderRepository
    participant CouponUsageRepository
    participant EventPublisher
    participant VnPayService

    Client->>OrderService: checkout(request, userEmail, httpRequest)
    activate OrderService

    OrderService->>OrderService: getUserByEmail(userEmail)
    OrderService->>AddressRepository: findByAddressIdAndUser_UserId()
    OrderService->>OrderService: deviceSessionService.getOrCreate()
    
    OrderService->>CartItemRepository: findAllByCartItemIdIn(request.cartItemIds)
    activate CartItemRepository
    CartItemRepository-->>OrderService: List<CartItemEntity>
    deactivate CartItemRepository

    loop Validate cart ownership
        OrderService->>OrderService: verify item belongs to user's cart
    end

    loop Validate product variant
        OrderService->>OrderService: verify isActive
        OrderService->>OrderService: check stockQuantity >= lockedStock + quantity
        OrderService->>OrderService: check quantity <= maxOrderQuantity
    end

    loop Validate & Apply Coupons (if any)
        OrderService->>CouponService: validateForCheckout(code, totalProductMoney, userEmail)
        CouponService-->>OrderService: CouponEntity
        OrderService->>CouponService: calculateDiscount(coupon, totalProductMoney)
        CouponService-->>OrderService: discountAmount
        OrderService->>OrderService: aggregate shop/shipping/payment discount
    end

    OrderService->>OrderService: Calculate totalWeightGram

    OrderService->>GhnShippingService: calculateFee(ShippingFeeRequest)
    activate GhnShippingService
    GhnShippingService-->>OrderService: GhnShippingFeeResponse (originalShippingFee)
    deactivate GhnShippingService

    OrderService->>OrderService: Calculate finalPaymentMoney

    loop Lock Inventory (Pessimistic Write via JPA)
        OrderService->>ProductVariantRepository: findById(variantId)
        ProductVariantRepository-->>OrderService: ProductVariantEntity
        OrderService->>OrderService: variant.lockedStock += quantity
        OrderService->>ProductVariantRepository: save(variant)
    end

    OrderService->>OrderService: build OrderEntity & OrderDetailEntities
    
    OrderService->>OrderRepository: save(order)
    activate OrderRepository
    OrderRepository-->>OrderService: savedOrder
    deactivate OrderRepository

    alt has applied coupons
        loop Save Coupon Usage
            OrderService->>CouponUsageRepository: save(CouponUsageEntity)
        end
    end

    OrderService->>CartItemRepository: deleteAllByCartItemIdIn(cartItemIds)

    OrderService->>EventPublisher: publishEvent(OrderCreatedEvent)

    alt request.paymentMethod == VNPAY
        OrderService->>VnPayService: createPaymentUrl(paymentRequest)
        activate VnPayService
        VnPayService-->>OrderService: paymentUrl
        deactivate VnPayService
        OrderService->>OrderService: setOrderStatus(PENDING_PAYMENT)
    else request.paymentMethod == COD
        OrderService->>OrderService: setOrderStatus(AWAITING_SHIPMENT)
    else other methods
        OrderService->>OrderService: setOrderStatus(PENDING_PAYMENT)
    end
    
    OrderService->>OrderRepository: save(savedOrder)

    OrderService-->>Client: CheckoutResponse
    deactivate OrderService
```

## 2. Get My Orders (`getMyOrders`)

```mermaid
sequenceDiagram
    participant Client
    participant OrderService
    participant UserRepository
    participant OrderRepository
    participant OrderMapper

    Client->>OrderService: getMyOrders(userEmail, pageable)
    activate OrderService

    OrderService->>UserRepository: findByEmail(userEmail)
    UserRepository-->>OrderService: UserEntity

    OrderService->>OrderRepository: findByUserOrderByOrderIdDesc(user, pageable)
    activate OrderRepository
    OrderRepository-->>OrderService: Page<OrderEntity>
    deactivate OrderRepository

    OrderService->>OrderMapper: map toSummaryResponse
    OrderMapper-->>OrderService: Page<OrderSummaryResponse>

    OrderService-->>Client: Page<OrderSummaryResponse>
    deactivate OrderService
```

## 3. Get My Order Detail (`getMyOrderDetail`)

```mermaid
sequenceDiagram
    participant Client
    participant OrderService
    participant UserRepository
    participant OrderRepository
    participant OrderMapper

    Client->>OrderService: getMyOrderDetail(orderId, userEmail)
    activate OrderService

    OrderService->>UserRepository: findByEmail(userEmail)
    UserRepository-->>OrderService: UserEntity

    OrderService->>OrderRepository: findByOrderIdAndUser(orderId, user)
    activate OrderRepository
    OrderRepository-->>OrderService: OrderEntity (or throw ORDER_NOT_FOUND)
    deactivate OrderRepository

    OrderService->>OrderMapper: toResponse(order)
    OrderMapper-->>OrderService: OrderResponse

    OrderService-->>Client: OrderResponse
    deactivate OrderService
```

## 4. Cancel My Order (`cancelMyOrder`)

```mermaid
sequenceDiagram
    participant Client
    participant OrderService
    participant OrderRepository
    participant ProductVariantRepository
    participant CouponUsageRepository

    Client->>OrderService: cancelMyOrder(orderId, request, userEmail)
    activate OrderService

    OrderService->>OrderService: getUserByEmail(userEmail)
    
    OrderService->>OrderRepository: findByOrderIdAndUser(orderId, user)
    activate OrderRepository
    OrderRepository-->>OrderService: OrderEntity
    deactivate OrderRepository

    alt orderStatus != PENDING
        OrderService-->>Client: throw AppException(CANCELLATION_NOT_ALLOWED)
    end

    loop Release locked_stock
        OrderService->>ProductVariantRepository: findById(variantId)
        ProductVariantRepository-->>OrderService: ProductVariantEntity
        OrderService->>OrderService: variant.lockedStock = max(0, lockedStock - quantity)
        OrderService->>ProductVariantRepository: save(variant)
    end

    OrderService->>CouponUsageRepository: deleteByOrderId(orderId)

    OrderService->>OrderService: order.setOrderStatus(CANCELLED)

    OrderService->>OrderRepository: save(order)
    activate OrderRepository
    OrderRepository-->>OrderService: savedOrder
    deactivate OrderRepository

    OrderService-->>Client: void
    deactivate OrderService
```

## 5. Get All Orders - Admin (`getAllOrders`)

```mermaid
sequenceDiagram
    participant Client
    participant OrderService
    participant OrderRepository
    participant OrderMapper

    Client->>OrderService: getAllOrders(pageable)
    activate OrderService

    OrderService->>OrderRepository: findAllByOrderByOrderIdDesc(pageable)
    activate OrderRepository
    OrderRepository-->>OrderService: Page<OrderEntity>
    deactivate OrderRepository

    OrderService->>OrderMapper: map toSummaryResponse
    OrderMapper-->>OrderService: Page<OrderSummaryResponse>

    OrderService-->>Client: Page<OrderSummaryResponse>
    deactivate OrderService
```

## 6. Get Order Detail - Admin (`getOrderDetail`)

```mermaid
sequenceDiagram
    participant Client
    participant OrderService
    participant OrderRepository
    participant OrderMapper

    Client->>OrderService: getOrderDetail(orderId)
    activate OrderService

    OrderService->>OrderRepository: findById(orderId)
    activate OrderRepository
    OrderRepository-->>OrderService: OrderEntity
    deactivate OrderRepository

    OrderService->>OrderMapper: toResponse(order)
    OrderMapper-->>OrderService: OrderResponse

    OrderService-->>Client: OrderResponse
    deactivate OrderService
```

## 7. Update Order Status - Admin (`updateOrderStatus`)

```mermaid
sequenceDiagram
    participant Client
    participant OrderService
    participant OrderRepository
    participant GhnOrderService

    Client->>OrderService: updateOrderStatus(orderId, request)
    activate OrderService

    OrderService->>OrderRepository: findById(orderId)
    activate OrderRepository
    OrderRepository-->>OrderService: OrderEntity
    deactivate OrderRepository

    OrderService->>OrderService: order.setOrderStatus(newStatus)

    alt newStatus == SHIPPING
        OrderService->>GhnOrderService: createOrder(order)
        activate GhnOrderService
        GhnOrderService-->>OrderService: GhnCreateOrderResponse (ghnOrder)
        deactivate GhnOrderService
        
        OrderService->>OrderService: order.setDeliveryOrderId(ghnOrder.orderCode)
        OrderService->>OrderService: order.setTrackingNumber(ghnOrder.orderCode)
        OrderService->>OrderService: order.setDeliveryStatus("READY_TO_PICK")
    end

    OrderService->>OrderRepository: save(order)
    activate OrderRepository
    OrderRepository-->>OrderService: savedOrder
    deactivate OrderRepository

    OrderService-->>Client: void
    deactivate OrderService
```
