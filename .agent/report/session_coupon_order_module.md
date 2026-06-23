# Session Report — Coupon & Order Module

**Date:** 2026-06-23
**Module:** Coupon · Order · OrderDetail · DeviceSession
**Status:** ✅ COMPLETED — `./mvnw.cmd compile` BUILD SUCCESS

---

## 1. Executive Summary

Phiên làm việc này implement đồng thời 3 business module lớn: **Coupon** (mã giảm giá), **Order** (đơn hàng), và **OrderDetail** (chi tiết đơn hàng), cùng với module hỗ trợ **DeviceSession** (theo dõi phiên thiết bị). Quyết định thiết kế quan trọng nhất là bỏ qua cơ chế Seller-owned Coupon (scope rõ ràng: Admin-only), giúp đơn giản hóa đáng kể luồng kiểm tra phân quyền.

**Key Achievements:**
- Implement Coupon Admin CRUD + 5-step validation (active → time → quota → per-user limit → min_order_amount).
- Implement Checkout flow 12-step atomic transaction với `@Transactional(rollbackFor = Exception.class)`.
- Tích hợp DeviceSession server-side (client **không gửi** deviceSessionId — server tự extract IP/UA từ `HttpServletRequest`).
- Tích hợp coupon vào checkout: tính discount theo 3 tầng (Shop, Shipping, Payment).
- Rollback coupon_usages + giải phóng locked_stock khi cancel đơn.
- Address ownership validation: `findByAddressIdAndUser_UserId` — bảo đảm user không dùng địa chỉ của người khác.
- Compile thành công 100%, không có lỗi.

---

## 2. Key Design Decisions

### 2.1 Seller bị loại bỏ
User yêu cầu: *"À thôi chúng ta sẽ bỏ qua seller ở bước này"*. Coupon không có `seller_id`, chỉ Admin mới tạo được. Schema V3.1 giữ nguyên không thêm cột.

### 2.2 DeviceSession — Server-side only
Client **không truyền** `deviceSessionId`. Server extract `IP + User-Agent` từ `HttpServletRequest`, hash SHA-256 tạo `sessionKey`, rồi getOrCreate `DeviceSessionEntity`. Logic được tách thành `DeviceSessionUtil` + `DeviceSessionService`.

### 2.3 Address — Embedded (no snapshot)
Address được tham chiếu trực tiếp tới `AddressEntity` (FK `address_id`). Không snapshot địa chỉ vào Order để tránh data duplication. `AddressResponse` được build từ entity trong `OrderMapper`.

### 2.4 Coupon discount 3-tầng
```
finalPaymentMoney = totalProductMoney
    - shopDiscountAmount      (SHOP_VOUCHER)
    + originalShippingFee
    - shippingDiscountAmount  (FREE_SHIPPING)
    - paymentDiscountAmount   (PAYMENT_VOUCHER)
```
Mỗi coupon chỉ áp dụng đúng 1 tầng dựa theo `CouponType`.

---

## 3. Database Migrations

| File | Nội dung |
|---|---|
| `V3__create_device_sessions_table.sql` | Bảng `device_sessions` (user tạo sẵn) |
| `V3.1__create_coupons_table.sql` | Bảng `coupons` (user tạo sẵn) |
| `V3.1.1__create_coupon_usages_table.sql` | Bảng `coupon_usages` (user tạo sẵn) |
| `V3.2__create_order_table.sql` | Bảng `orders` (user tạo sẵn) |
| `V3.3__create_order_details_table.sql` | Bảng `order_details` (user tạo sẵn) |
| `V3.4__seed_coupon_order_permissions.sql` | Seed 6 permissions: `coupon:manage`, `coupon:apply`, `order:create`, `order:view`, `order:manage`, `order:admin`. Gán USER + ADMIN. |

> **Note:** Tất cả schema migrations (V3 → V3.3) do user chuẩn bị sẵn. AI chỉ tạo V3.4 permissions.

---

## 4. Các Files Đã Tạo / Chỉnh Sửa

### 4.1 New Files — Enums (Phase 0)

| File | Các values |
|---|---|
| `enums/CouponType.java` | `SHOP_VOUCHER`, `FREE_SHIPPING`, `PAYMENT_VOUCHER` |
| `enums/DiscountType.java` | `PERCENTAGE`, `FIXED_AMOUNT` |
| `enums/OrderStatus.java` | `PENDING`, `PENDING_PAYMENT`, `AWAITING_SHIPMENT`, `SHIPPING`, `DELIVERED`, `CANCELLED`, `REFUNDING`, `REFUNDED` |
| `enums/PaymentMethod.java` | `COD`, `VNPAY`, `MOMO`, `BANK_TRANSFER` |
| `enums/PaymentStatus.java` | `UNPAID`, `PAID`, `REFUNDED`, `FAILED` |
| `enums/ShippingMethod.java` | `STANDARD`, `EXPRESS`, `ECONOMY` |

### 4.2 New Files — Entities & Repositories (Phase 1)

| Layer | File | Mô tả |
|---|---|---|
| **Util** | `util/DeviceSessionUtil.java` | SHA-256 hash của `IP + UserAgent` → `sessionKey` |
| **Service** | `service/device/DeviceSessionService.java` | Interface: `getOrCreate(HttpServletRequest, UserEntity)` |
| **Service Impl** | `service/device/impl/DeviceSessionServiceImpl.java` | Lookup by sessionKey → tạo mới nếu chưa có |
| **Entity** | `entity/DeviceSessionEntity.java` | `sessionKey`, `ipAddress`, `userAgent`, `@ManyToOne(UserEntity)` |
| **Repository** | `repository/DeviceSessionRepository.java` | `findBySessionKey(String)` |
| **Entity** | `entity/CouponEntity.java` | Full coupon schema: discountType, discountValue, maxDiscountAmount, minOrderAmount, maxUsage, userUsageLimit, startDate, endDate, isActive, couponType |
| **Repository** | `repository/CouponRepository.java` | `findByCode`, `existsByCode`, `findAllByOrderByCouponIdDesc` |
| **Entity** | `entity/CouponUsageEntity.java` | `@ManyToOne(UserEntity)`, `@ManyToOne(CouponEntity)`, `orderId` |
| **Repository** | `repository/CouponUsageRepository.java` | `countByCoupon`, `countByCouponAndUser`, `deleteByOrderId` |
| **Entity** | `entity/OrderEntity.java` | Full order: discount breakdown (5 fields), FK `address_id`, FK `device_session_id`, `@OneToMany(OrderDetailEntity)` |
| **Repository** | `repository/OrderRepository.java` | `findByUserOrderByOrderIdDesc`, `findByOrderIdAndUser`, `findAllByOrderByOrderIdDesc` |
| **Entity** | `entity/OrderDetailEntity.java` | `@ManyToOne(OrderEntity)`, `@ManyToOne(ProductVariantEntity)`, `numOfProduct`, `orderDetailPrice`, `totalMoney`, `itemStatus` |
| **Repository** | `repository/OrderDetailRepository.java` | JpaRepository standard |

### 4.3 New Files — DTOs (Phase 3)

| Package | File | Dùng cho |
|---|---|---|
| `dtos/request/coupon` | `CouponCreateRequest.java` | Admin tạo coupon |
| `dtos/request/coupon` | `CouponUpdateRequest.java` | Admin cập nhật coupon (code là immutable) |
| `dtos/request/coupon` | `CouponApplyRequest.java` | User preview discount |
| `dtos/response/coupon` | `CouponResponse.java` | Full coupon detail |
| `dtos/response/coupon` | `CouponApplyResponse.java` | Kết quả preview: originalAmount, discountAmount, finalAmount |
| `dtos/request/order` | `CheckoutRequest.java` | Đặt hàng (không có deviceSessionId — server tự extract) |
| `dtos/request/order` | `OrderStatusUpdateRequest.java` | Admin cập nhật status |
| `dtos/request/order` | `OrderCancelRequest.java` | User hủy đơn kèm lý do |
| `dtos/response/order` | `AddressResponse.java` | Địa chỉ giao hàng embedded trong OrderResponse |
| `dtos/response/order` | `OrderDetailResponse.java` | Chi tiết từng sản phẩm trong đơn |
| `dtos/response/order` | `OrderResponse.java` | Full order detail + address + items |
| `dtos/response/order` | `OrderSummaryResponse.java` | Rút gọn cho list view |
| `dtos/response/order` | `CheckoutResponse.java` | Kết quả checkout: orderId, orderStatus, finalPaymentMoney, paymentUrl |

### 4.4 New Files — Mappers (Phase 4)

| File | Mô tả |
|---|---|
| `mapper/CouponMapper.java` | `toEntity(CreateRequest)`, `toResponse(Entity)`, `toApplyResponse(Entity, BigDecimal, BigDecimal)` |
| `mapper/OrderMapper.java` | `toAddressResponse`, `toDetailResponse`, `toResponse`, `toSummaryResponse` |

### 4.5 New Files — Service Layer (Phase 5)

| File | Mô tả |
|---|---|
| `service/coupon/CouponService.java` | Interface: CRUD + previewApply + validateForCheckout (internal) + suggestCoupons (TODO stub) |
| `service/coupon/impl/CouponServiceImpl.java` | Implement 5-step validate, discount calc (PERCENTAGE vs FIXED_AMOUNT), Admin CRUD |
| `service/order/OrderService.java` | Interface: checkout, getMyOrders, getMyOrderDetail, cancelMyOrder, getAllOrders, getOrderDetail, updateOrderStatus |
| `service/order/impl/OrderServiceImpl.java` | Checkout 12-step, cancel với inventory rollback + coupon rollback |

### 4.6 New Files — Controllers (Phase 6)

| File | Endpoints |
|---|---|
| `controller/CouponController.java` | 6 endpoints: POST `/coupons`, GET `/coupons`, GET `/coupons/{id}`, PUT `/coupons/{id}`, DELETE `/coupons/{id}`, POST `/coupons/preview` |
| `controller/OrderController.java` | 8 endpoints + GHN webhook stub: POST `/orders/checkout`, GET `/orders/my`, GET `/orders/my/{orderId}`, PUT `/orders/my/{orderId}/cancel`, GET `/orders`, GET `/orders/{orderId}`, PUT `/orders/{orderId}/status`, POST `/orders/webhooks/ghn` |

### 4.7 Modified Files

| File | Thay đổi |
|---|---|
| `entity/ProductVariantEntity.java` | Thêm `@OneToMany(mappedBy = "variant") List<OrderDetailEntity> orderDetails` |
| `repository/AddressRepository.java` | Thêm `findByAddressIdAndUser_UserId(Long, Long)` — validate address ownership khi checkout |
| `repository/CartItemRepository.java` | Thêm `findAllByCartItemIdIn(List<Long>)` và `deleteAllByCartItemIdIn(List<Long>)` — bulk ops cho checkout |
| `exception/ErrorCode.java` | Thêm Coupon range (9001–9007) và Order range (10001–10008) |
| `resources/messages.properties` | Thêm ~50 message keys cho Coupon + Order (error, success, validation) |

---

## 5. API Endpoints

### 5.1 Coupon Endpoints

**Base URL:** `/api/v1/coupons`

| Method | Path | Permission | Role | Mô tả |
|---|---|---|---|---|
| `POST` | `/` | `coupon:manage` | ADMIN | Tạo mã giảm giá |
| `GET` | `/` | `coupon:manage` | ADMIN | Danh sách tất cả mã (phân trang) |
| `GET` | `/{id}` | `coupon:manage` | ADMIN | Chi tiết mã theo ID |
| `PUT` | `/{id}` | `coupon:manage` | ADMIN | Cập nhật mã (code immutable) |
| `DELETE` | `/{id}` | `coupon:manage` | ADMIN | Vô hiệu hóa mã (soft delete: `isActive = false`) |
| `POST` | `/preview` | `coupon:apply` | USER | Preview discount trước khi checkout |

### 5.2 Order Endpoints

**Base URL:** `/api/v1/orders`

| Method | Path | Permission | Role | Mô tả |
|---|---|---|---|---|
| `POST` | `/checkout` | `order:create` | USER | Đặt hàng từ cart items |
| `GET` | `/my` | `order:view` | USER | Danh sách đơn hàng của user |
| `GET` | `/my/{orderId}` | `order:view` | USER | Chi tiết đơn hàng (owner-scoped) |
| `PUT` | `/my/{orderId}/cancel` | `order:manage` | USER | Hủy đơn (chỉ khi PENDING) |
| `GET` | `/` | `order:admin` | ADMIN | Tất cả đơn hàng (phân trang) |
| `GET` | `/{orderId}` | `order:admin` | ADMIN | Chi tiết bất kỳ đơn hàng |
| `PUT` | `/{orderId}/status` | `order:admin` | ADMIN | Cập nhật trạng thái đơn |
| `POST` | `/webhooks/ghn` | public | - | GHN webhook stub (TODO) |

---

## 6. Business Logic Chi Tiết

### 6.1 Coupon 5-Step Validation

```
Step 1: findByCode + isActive == true         → COUPON_NOT_FOUND nếu không hợp lệ
Step 2: startDate <= now <= endDate           → COUPON_EXPIRED
Step 3: countByCoupon < maxUsage              → COUPON_EXHAUSTED
Step 4: countByCouponAndUser < userUsageLimit → COUPON_USAGE_LIMIT_EXCEEDED
Step 5: orderAmount >= minOrderAmount         → INADEQUATE_ORDER_VALUE
```

### 6.2 Coupon Discount Calculation

```java
// PERCENTAGE
raw = orderAmount * discountValue / 100
discount = min(raw, maxDiscountAmount)   // bắt buộc có maxDiscountAmount

// FIXED_AMOUNT
discount = min(discountValue, orderAmount)  // không giảm quá tổng đơn
```

### 6.3 Checkout 12-Step Atomic Flow

```
Step 1:  Resolve User by email (JWT subject)
Step 2:  Validate Address ownership (findByAddressIdAndUser_UserId)
Step 3:  Extract DeviceSession từ HttpServletRequest (server-side)
Step 4:  Load CartItems by IDs + validate ownership (cart.user == current user)
Step 5:  Re-validate từng variant: isActive, available stock, maxOrderQuantity
Step 6:  Validate coupon (nếu có) qua CouponService.validateForCheckout
Step 7:  Tính toán tài chính: 5-field discount breakdown
Step 8:  Lock inventory: lockedStock += quantity (cho từng variant)
Step 9:  Save OrderEntity + List<OrderDetailEntity>
Step 10: Save CouponUsageEntity (nếu có coupon)
Step 11: Delete CartItems đã checkout (deleteAllByCartItemIdIn)
Step 12: Payment routing: COD → AWAITING_SHIPMENT | Online → PENDING_PAYMENT
```

### 6.4 Cancel Order Flow

```
1. Validate ownership (findByOrderIdAndUser)
2. Validate trạng thái: chỉ cho hủy khi PENDING
3. Giải phóng lockedStock cho từng OrderDetail
4. Rollback CouponUsage (deleteByOrderId)
5. TODO: nếu paymentStatus == PAID → gọi Refund API
6. Set orderStatus = CANCELLED
```

---

## 7. Permission Mapping

| Permission | Role | Mô tả |
|---|---|---|
| `coupon:manage` | ADMIN | Toàn quyền CRUD coupon |
| `coupon:apply` | USER, ADMIN | Preview/apply coupon discount |
| `order:create` | USER, ADMIN | Đặt hàng (checkout) |
| `order:view` | USER, ADMIN | Xem đơn hàng của bản thân |
| `order:manage` | USER, ADMIN | Hủy đơn của bản thân |
| `order:admin` | ADMIN | Xem + cập nhật tất cả đơn hàng |

---

## 8. Error Codes

### Coupon Range (9xxx)

| Code | ErrorCode | HTTP | Mô tả |
|---|---|---|---|
| 9001 | `COUPON_NOT_FOUND` | 404 | Mã không tồn tại hoặc đã bị tắt |
| 9002 | `COUPON_CODE_ALREADY_EXISTS` | 409 | Trùng mã khi tạo mới |
| 9003 | `COUPON_EXPIRED` | 400 | Ngoài thời gian hiệu lực |
| 9004 | `COUPON_EXHAUSTED` | 400 | Đã dùng hết quota tổng |
| 9005 | `COUPON_USAGE_LIMIT_EXCEEDED` | 400 | User đã dùng quá giới hạn cá nhân |
| 9006 | `INADEQUATE_ORDER_VALUE` | 400 | Tổng đơn chưa đạt min_order_amount |
| 9007 | `BUDGET_EXCEEDED` | 400 | PERCENTAGE coupon thiếu maxDiscountAmount |

### Order Range (10xxx)

| Code | ErrorCode | HTTP | Mô tả |
|---|---|---|---|
| 10001 | `ORDER_NOT_FOUND` | 404 | Đơn hàng không tồn tại |
| 10002 | `ORDER_STATE_CONFLICT` | 409 | Đơn đã ở trạng thái này rồi |
| 10003 | `CANCELLATION_NOT_ALLOWED` | 400 | Không thể hủy ở trạng thái hiện tại |
| 10004 | `INVALID_ORDER_STATE` | 400 | Chuyển trạng thái không hợp lệ |
| 10005 | `CHECKOUT_CONCURRENCY` | 409 | Race condition khi checkout |
| 10006 | `ORDER_OWNERSHIP_DENIED` | 403 | Không có quyền truy cập đơn này |
| 10007 | `CART_ITEMS_NOT_FOUND` | 400 | Cart items không tìm thấy hoặc không thuộc user |
| 10008 | `ADDRESS_NOT_FOUND` | 404 | Địa chỉ không tồn tại hoặc không thuộc user |

---

## 9. Issues & Fixes

### Issue 1: messages.properties trailing newline
**Vấn đề:** `replace_file_content` không thể thêm nội dung vào cuối file do dòng trống cuối file.
**Fix:** Dùng PowerShell `Add-Content` thay thế.

### Issue 2: CartItemRepository — Unused import warning
**Vấn đề:** Import `CartEntity` được thêm vào nhưng không dùng khi add bulk query methods.
**Fix:** Remove ngay sau khi IDE phát hiện lint.

---

## 10. TODO / Known Limitations

| Item | Mô tả |
|---|---|
| **GHN Shipping Fee** | `OrderServiceImpl.checkout()` step 7: `originalShippingFee = BigDecimal.ZERO`. Cần gọi GHN API để tính phí ship thực tế. |
| **VNPAY / MoMo** | `CheckoutResponse.paymentUrl = null` với Online payment. Cần tích hợp payment gateway. |
| **Refund API** | `cancelMyOrder()`: nếu `paymentStatus == PAID` cần gọi cổng thanh toán để hoàn tiền. |
| **GHN Webhook** | `POST /api/v1/orders/webhooks/ghn` là stub rỗng — cần verify signature + parse payload. |
| **Coupon Suggestion** | `suggestCoupons()` trả về empty list — cần implement recommendation engine. |
| **Pessimistic Locking** | `lockedStock` update hiện dùng `@Transactional` thông thường. Production nên thêm `@Lock(PESSIMISTIC_WRITE)` trên variant query để tránh race condition dưới high concurrency. |

---

## 11. Architecture Notes

- **Atomicity:** Checkout dùng `@Transactional(rollbackFor = Exception.class)` — đảm bảo rollback toàn bộ nếu bất kỳ bước nào fail (stock lock, coupon record, cart cleanup).
- **Separation:** `CouponService.validateForCheckout()` là internal method được `OrderServiceImpl` gọi trong cùng transaction, đảm bảo consistency.
- **Compile Verification:** `./mvnw.cmd compile` — BUILD SUCCESS, 0 errors.

---

## 12. Dependencies Cross-Module

| Module phụ thuộc | Lý do |
|---|---|
| `User` | Lookup user bằng email từ JWT subject |
| `Product / Variant` | Validate isActive, available stock, maxOrderQuantity; lock lockedStock |
| `Cart / CartItem` | Đọc cart items khi checkout, xóa sau khi đặt hàng thành công |
| `Address` | Validate ownership và embed vào OrderResponse |
| `DeviceSession` | Ghi nhận phiên thiết bị cho mỗi đơn hàng |
