# Sequence Diagrams for Order Service

Tài liệu này chứa các sơ đồ tuần tự cho tất cả các hoạt động trong `OrderServiceImpl`.

## 1. Thanh toán (`checkout`)

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

    loop Xác thực quyền sở hữu giỏ hàng
        OrderService->>OrderService: xác minh sản phẩm thuộc về giỏ hàng của người dùng
    end

    loop Xác thực biến thể sản phẩm
        OrderService->>OrderService: xác minh isActive
        OrderService->>OrderService: kiểm tra stockQuantity >= lockedStock + quantity
        OrderService->>OrderService: kiểm tra quantity <= maxOrderQuantity
    end

    loop Xác thực & Áp dụng Mã giảm giá (nếu có)
        OrderService->>CouponService: validateForCheckout(code, totalProductMoney, userEmail)
        CouponService-->>OrderService: CouponEntity
        OrderService->>CouponService: calculateDiscount(coupon, totalProductMoney)
        CouponService-->>OrderService: discountAmount
        OrderService->>OrderService: tổng hợp giảm giá của shop/vận chuyển/thanh toán
    end

    OrderService->>OrderService: Tính toán totalWeightGram

    OrderService->>GhnShippingService: calculateFee(ShippingFeeRequest)
    activate GhnShippingService
    GhnShippingService-->>OrderService: GhnShippingFeeResponse (originalShippingFee)
    deactivate GhnShippingService

    OrderService->>OrderService: Tính toán finalPaymentMoney

    loop Khóa Kho hàng (Pessimistic Write qua JPA)
        OrderService->>ProductVariantRepository: findById(variantId)
        ProductVariantRepository-->>OrderService: ProductVariantEntity
        OrderService->>OrderService: variant.lockedStock += quantity
        OrderService->>ProductVariantRepository: save(variant)
    end

    OrderService->>OrderService: xây dựng OrderEntity & OrderDetailEntities
    
    OrderService->>OrderRepository: save(order)
    activate OrderRepository
    OrderRepository-->>OrderService: savedOrder
    deactivate OrderRepository

    alt có áp dụng mã giảm giá
        loop Lưu lịch sử sử dụng Mã giảm giá (CouponUsageEntity)
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
    else các phương thức khác
        OrderService->>OrderService: setOrderStatus(PENDING_PAYMENT)
    end
    
    OrderService->>OrderRepository: save(savedOrder)

    OrderService-->>Client: CheckoutResponse
    deactivate OrderService
```

## 2. Lấy Đơn hàng của tôi (`getMyOrders`)

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

    OrderService->>OrderMapper: ánh xạ thành toSummaryResponse
    OrderMapper-->>OrderService: Page<OrderSummaryResponse>

    OrderService-->>Client: Page<OrderSummaryResponse>
    deactivate OrderService
```

## 3. Lấy Chi tiết Đơn hàng của tôi (`getMyOrderDetail`)

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
    OrderRepository-->>OrderService: OrderEntity (hoặc ném ra ORDER_NOT_FOUND)
    deactivate OrderRepository

    OrderService->>OrderMapper: toResponse(order)
    OrderMapper-->>OrderService: OrderResponse

    OrderService-->>Client: OrderResponse
    deactivate OrderService
```

## 4. Hủy Đơn hàng của tôi (`cancelMyOrder`)

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

    loop Giải phóng locked_stock
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

## 5. Lấy Tất cả Đơn hàng - Admin (`getAllOrders`)

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

    OrderService->>OrderMapper: ánh xạ thành toSummaryResponse
    OrderMapper-->>OrderService: Page<OrderSummaryResponse>

    OrderService-->>Client: Page<OrderSummaryResponse>
    deactivate OrderService
```

## 6. Lấy Chi tiết Đơn hàng - Admin (`getOrderDetail`)

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

## 7. Cập nhật Trạng thái Đơn hàng - Admin (`updateOrderStatus`)

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
