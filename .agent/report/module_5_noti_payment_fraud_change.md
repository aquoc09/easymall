# 🛡️ Báo cáo: Tác động của thay đổi `orders` table đến Fraud Detection

> **Ngày:** 2026-07-07  
> **Liên quan:** Migration `V5.10__alter_orders.sql` · `FraudDetectionServiceImpl` · `OrderRepository`

---

## 1. Bối cảnh thay đổi

Migration **V5.10** xoá cột `device_session_id` khỏi bảng `orders` để đơn giản hoá schema — `OrderEntity` không còn FK đến `device_sessions`. Điều này gây ra **startup crash** và làm mất một feature đang hoạt động của Fraud Detection.

---

## 2. Lỗi khởi động (đã fix)

### Error chain
```
SpringApplication fails
 └─► GhnController
      └─► GhnWebhookServiceImpl (inject OrderRepository)
           └─► OrderRepository.countByDeviceSessionAndOrderDateAfter(...)
                └─► ❌ No property 'deviceSession' found for type 'OrderEntity'
```

### Nguyên nhân
Spring Data JPA **tự động sinh SQL từ tên method** (Derived Query). Khi app khởi động nó parse `countByDeviceSessionAndOrderDateAfter` và cố tìm `deviceSession` trên `OrderEntity` — field đã bị xoá → fail ngay lúc startup, không cần gọi API.

### Fix đã áp dụng

| File | Thay đổi |
|------|---------|
| `OrderRepository.java` | Xoá method `countByDeviceSessionAndOrderDateAfter`, bỏ import `DeviceSessionEntity`, `LocalDate` |
| `FraudDetectionServiceImpl.java` | Thay call repository bằng `ordersPerDevice24h = 1` (mock), xoá field + import `OrderRepository` |

---

## 3. Feature bị ảnh hưởng: `orders_per_device_24h`

### Vai trò trong mô hình ML

`orders_per_device_24h` là **một trong 9 features** được gửi lên AI fraud service:

```java
FraudRequestDTO.builder()
    .orderTotalAmount(...)
    .paymentMethod(...)
    .isVpnProxy(...)
    .locationMismatch(...)
    .ordersPerDevice24h(...)   // ← Feature bị ảnh hưởng
    .accountAgeDays(...)
    .reputationScore(...)
    .failedPaymentAttempts10m(...)
    .totalDistinctDevices(...)
    .returnRate(...)
    .build();
```

Feature này đo **số đơn hàng được đặt từ cùng một thiết bị trong vòng 24 giờ** — dấu hiệu quan trọng để phát hiện bulk-order fraud, account takeover, và card testing.

### Trước khi thay đổi (logic cũ)
```java
// Truy vấn trực tiếp qua device_session_id trong bảng orders
int ordersPerDevice24h = orderRepository
    .countByDeviceSessionAndOrderDateAfter(deviceSession, LocalDate.now().minusDays(1));
```

### Sau khi thay đổi (hiện tại — mock)
```java
// TODO: device_session_id đã bị xoá khỏi OrderEntity.
// Tạm thời mock = 1 để không ảnh hưởng fraud score.
int ordersPerDevice24h = 1;
```

---

## 4. Đánh giá mức độ ảnh hưởng

| Hạng mục | Trước | Sau (hiện tại) | Mức độ |
|---------|-------|---------------|--------|
| App startup | ❌ Crash | ✅ Hoạt động | ✅ Đã fix |
| `orders_per_device_24h` accuracy | ✅ Chính xác | ⚠️ Luôn = 1 | 🟡 Degraded |
| `isVpnProxy` | ✅ Từ deviceSession | ✅ Vẫn pass qua param | ✅ Hoạt động |
| `locationMismatch` | 🟡 Mock = 0 (chưa implement) | 🟡 Mock = 0 | Không đổi |
| `totalDistinctDevices` | ✅ Từ user_stats | ✅ Từ user_stats | ✅ Hoạt động |
| Fraud score tổng thể | Đầy đủ 9 features | Thiếu 1 feature thực | 🟡 Giảm độ chính xác |

> **Kết luận:** Model vẫn chạy, nhưng thiếu signal `orders_per_device_24h` — các trường hợp fraud liên quan đến **device-based bulk ordering** sẽ không bị phát hiện chính xác.

---

## 5. Các options để implement lại

### Option A: Truy vấn qua `user_id` (đơn giản, đã có data)
```java
// OrderRepository thêm method mới:
int countByUserAndOrderDateAfter(UserEntity user, OffsetDateTime after);

// Dùng trong FraudDetectionServiceImpl:
ordersPerDevice24h = orderRepository
    .countByUserAndOrderDateAfter(user, OffsetDateTime.now().minusHours(24));
```
**Ưu:** Đơn giản, không cần DB thêm.  
**Nhược:** Đo per-user thay vì per-device — kém chính xác hơn (không phát hiện được khi attacker dùng nhiều account trên 1 thiết bị).

---

### Option B: Thêm `device_fingerprint` vào `orders` (recommended)
```sql
-- Migration mới:
ALTER TABLE orders ADD COLUMN device_fingerprint VARCHAR(255) NULL;
```
```java
// OrderEntity thêm field:
@Column(name = "device_fingerprint", length = 255)
private String deviceFingerprint;

// OrderRepository:
int countByDeviceFingerprintAndOrderDateAfter(String fingerprint, OffsetDateTime after);
```
Frontend gửi `deviceFingerprint` khi checkout → lưu vào order → query được.  
**Ưu:** Khôi phục đầy đủ semantics cũ, không phụ thuộc `device_sessions`.  
**Nhược:** Cần thêm migration + contract API mới.

---

### Option C: Giữ mock, không implement lại
Chấp nhận feature này luôn = 1. Phù hợp nếu:
- Fraud model vẫn đủ chính xác với 8 features còn lại
- Scope MVP chưa cần phát hiện device-based fraud

---

## 6. Các feature còn bị mock (chưa implement)

| Feature | Hiện tại | Ghi chú |
|---------|---------|---------|
| `ordersPerDevice24h` | Mock = 1 | **Vừa bị break** — xem options bên trên |
| `locationMismatch` | Mock = 0 | Cần so sánh billing address vs IP geolocation |

---

## 7. Callsite trong production flow

```
POST /api/v1/orders/checkout
  └─► OrderServiceImpl.placeOrder()
       ├─► Step 9: Tạo order (OrderEntity saved)
       ├─► Step 10: Commit CouponUsage
       ├─► Step 11: Xoá CartItems
       ├─► Step 12: fraudDetectionService.evaluateOrder(order, deviceSession, user)
       │    └─► evaluateOrder vẫn nhận deviceSession qua parameter
       │         └─► isVpnProxy được extract từ deviceSession ✅
       │         └─► ordersPerDevice24h = 1 (mock) ⚠️
       └─► Step 13: Payment routing theo SystemDecision
```

> **Note:** `deviceSession` vẫn được truyền vào `evaluateOrder()` qua parameter — chỉ bị mất tại bước query DB. `isVpnProxy` feature vẫn **hoạt động đúng**.

---

## 8. Hành động được đề xuất

- [ ] **Quyết định Option A/B/C** (xem mục 5)
- [ ] Nếu chọn Option B: tạo migration `V5.14__add_device_fingerprint_to_orders.sql`
- [ ] Implement lại `locationMismatch` (IP geolocation vs shipping address)
- [ ] Viết unit test cho `FraudDetectionServiceImpl.evaluateOrder()` để cover các edge case sau thay đổi

---

## 9. Implementation Notes — 2 bảng mới (Brand New)

> Các bảng này đã có **schema + entity**, chưa có **business logic**. Phần này mô tả chi tiết cần implement gì.

---

### 9.1 `payments` — Lịch sử Thanh toán

**Files đã tạo:**
- Migration: [`V5.12__create_payments.sql`](file:///d:/Study/DoAn/DATN/easymall/src/main/resources/db/migration/V5.12__create_payments.sql)
- Entity: [`PaymentEntity.java`](file:///d:/Study/DoAn/DATN/easymall/src/main/java/com/quocnva/easymall/entity/PaymentEntity.java)

**Kiến trúc cần xây dựng:**

```
PaymentService (interface)
  └─► PaymentServiceImpl
        ├─► initPayment(order)         → tạo record PENDING
        ├─► handleVnpayCallback(...)   → cập nhật PAID/FAILED
        ├─► handleMomoCallback(...)    → cập nhật PAID/FAILED
        └─► refund(orderId)            → tạo record REFUNDED

PaymentRepository extends JpaRepository<PaymentEntity, Long>
  └─► findByOrderOrderByCreatedAtDesc(OrderEntity order)
  └─► findTopByOrderAndPaymentStatus(OrderEntity order, String status)
```

**Tích hợp vào Order Flow:**

```
OrderServiceImpl.placeOrder()
  └─► Step 12 (sau Fraud Detection):
       └─► [COD]  paymentService.initPayment(order, COD)   → PENDING, paid_at = null
       └─► [VNPAY] paymentService.initPayment(order, VNPAY) → PENDING, return paymentUrl
       └─► [MOMO]  paymentService.initPayment(order, MOMO)  → PENDING, return paymentUrl

GhnWebhookServiceImpl.handleWebhook():
  └─► Khi status = "delivered" → Order COMPLETED
       └─► Nếu COD: paymentService.markPaid(order)  → cập nhật PAID, paid_at = now
```

**Endpoints cần tạo:**

| Method | Path | Mô tả |
|--------|------|---------|
| `GET` | `/api/v1/payments/{orderId}` | User xem lịch sử thanh toán của order |
| `POST` | `/api/v1/payments/vnpay/callback` | VNPAY IPN webhook |
| `POST` | `/api/v1/payments/momo/callback` | MoMo IPN webhook |

**DTOs cần tạo:**
```
dtos/response/payment/
  └─► PaymentResponse.java      (paymentId, amount, status, paidAt, transactionId)
dtos/request/payment/
  └─► VnpayCallbackRequest.java
  └─► MomoCallbackRequest.java
```

**Business rules:**
- Một order có thể có nhiều payment records (retry sau FAILED)
- Chỉ có tối đa 1 record PAID tại mỗi thời điểm
- REFUND tạo record mới, không update record cũ
- Verify chữ ký (HMAC) từ VNPAY/MoMo trước khi cập nhật trạng thái

---

### 9.2 `notifications` — Thông báo In-App

**Files đã tạo:**
- Migration: [`V5.13__create_notifications.sql`](file:///d:/Study/DoAn/DATN/easymall/src/main/resources/db/migration/V5.13__create_notifications.sql)
- Entity: [`NotificationEntity.java`](file:///d:/Study/DoAn/DATN/easymall/src/main/java/com/quocnva/easymall/entity/NotificationEntity.java)

**Kiến trúc cần xây dựng:**

```
NotificationService (interface)
  └─► NotificationServiceImpl
        ├─► send(userId, type, title, content, referenceId)
        ├─► getUnread(userId, pageable)
        ├─► markAsRead(notificationId, userId)
        └─► markAllAsRead(userId)

NotificationRepository extends JpaRepository<NotificationEntity, Long>
  └─► findByUserAndIsReadFalseOrderByCreatedAtDesc(UserEntity, Pageable)
  └─► findByUserOrderByCreatedAtDesc(UserEntity, Pageable)
  └─► @Modifying countByUserAndIsReadFalse(UserEntity)  ← badge count
```

**Tích hợp hook vào Order Flow:**

```
GhnWebhookServiceImpl.handleWebhook()
  └─► Sau khi update deliveryStatus/orderStatus:
       └─► notificationService.send(
              userId        = order.getUser().getUserId(),
              type          = "ORDER_UPDATE",
              title         = "Đơn hàng #" + orderId + " đã được cập nhật",
              content       = "Trạng thái: " + deliveryStatus,
              referenceId   = orderId
            )

OrderServiceImpl.placeOrder()
  └─► Sau khi order được xác nhận:
       └─► notificationService.send(userId, "ORDER_UPDATE", "Đã nhận đơn", ..., orderId)
```

**Endpoints cần tạo:**

| Method | Path | Mô tả |
|--------|------|---------|
| `GET` | `/api/v1/notifications` | Lấy danh sách thông báo (phân trang) |
| `GET` | `/api/v1/notifications/unread-count` | Đếm badge chưa đọc |
| `PATCH` | `/api/v1/notifications/{id}/read` | Đánh dấu 1 thông báo đã đọc |
| `PATCH` | `/api/v1/notifications/read-all` | Đánh dấu tất cả đã đọc |

**DTOs cần tạo:**
```
dtos/response/notification/
  └─► NotificationResponse.java  (notificationId, title, content, type, referenceId, isRead, createdAt)
  └─► UnreadCountResponse.java   (count: int)
```

**Business rules:**
- `markAsRead` phải verify `user_id` để tránh IDOR (user A đọc notification của user B)
- Dùng `@Modifying` native query cho `markAllAsRead` — không load toàn bộ entity
- Chỉ index `(user_id, is_read)` — không cần index riêng `created_at` vì query luôn filter bằng user_id trước
- Có thể thiết kế thêm WebSocket push sau này (Server-Sent Events hoặc SockJS)

**Các `notification_type` được định nghĩa:**

| Type | Kích hoạt khi nào |
|------|--------------------|
| `ORDER_UPDATE` | GHN webhook update delivery status |
| `ORDER_CONFIRMED` | Đơn được xác nhận sau placeOrder |
| `PAYMENT_SUCCESS` | VNPAY/MoMo callback thành công |
| `SYSTEM` | Admin push thông báo bảo trì, cập nhật |
| `PROMOTION` | Voucher mới / flash sale |
| `REVIEW_REPLY` | Seller/admin trả lời review của user |
