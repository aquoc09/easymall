# 🛒 PHẦN 1: CỤM CHECKOUT

## (Nghiệp vụ đặt hàng nguyên tử – Atomic Checkout)

> ⚠️ **BẮT BUỘC**: Toàn bộ quy trình checkout phải nằm trong:
>
> ```java
> @Transactional(rollbackFor = Exception.class)
> ```
>
> Hỏng ở bước nào → **rollback toàn bộ** các bước trước đó.

---

## 🧠 Bước 1: Re-Validation (Xác thực chốt chặn)

### Hành động

Frontend gửi lên:

- `List<CartItemIds>`
- `address_id`
- `coupon_code` (nếu có)

### Logic Backend

Re-check toàn bộ dữ liệu **tại thời điểm checkout**:

- Shop có đang hoạt động không?
- Sản phẩm có bị ẩn / xóa không?
- `stock_quantity - locked_stock` có đủ không?
- Coupon còn lượt sử dụng không?

---

## 🚚 Bước 2: Shipping Sync (Đồng bộ GHN)

### Hành động

Gọi API GHN:

```
POST /v2/shipping-order/fee
```

### Payload

- `pickup_address` (địa chỉ shop)
- `delivery_address` (địa chỉ khách)
- Tổng `weight`, `length`, `width`, `height`

### Kết quả

- Nhận `shipping_fee` từ GHN
- ❌ **Không bao giờ** tin phí ship từ Frontend

---

## 🧮 Bước 3: Atomic Calculation (Kế toán thời gian thực)

### Hành động

Tính toán và **chốt sổ tài chính**, ghi vào bảng `orders`.

### Công thức chuẩn (Bọc thép)

1. **Subtotal**

   ```
   = Σ (price × quantity)
   ```

2. **Base Amount**

   ```
   = Subtotal - Discount_Amount (Coupon Shop)
   ```

3. **Total Amount (Khách trả)**

   ```
   = Base_Amount + Shipping_Fee
   ```

4. **Platform Fee (Sàn thu)**

   ```
   = Base_Amount × (Commission_Rate / 100)
   ```

5. **Tax Amount (Thuế)**

   ```
   = Base_Amount × (Tax_Rate / 100)
   ```

6. **Settlement Amount (Shop nhận)**
   ```
   = Base_Amount - Platform_Fee - Tax_Amount
   ```

### Lưu ý quan trọng

- `variant_info` (JSONB) trong `order_items` **bắt buộc** snapshot:
  - Tên sản phẩm
  - Giá
  - Hình ảnh  
    → Phục vụ **đối soát & tranh chấp** nếu Shop thay đổi dữ liệu sau này.

---

## 🔒 Bước 4: Inventory Lock & DB Write (Khóa tài nguyên)

### Logic Kho

```
locked_stock = locked_stock + quantity
```

### Logic Coupon

- `INSERT` vào `coupon_usages` (giữ chỗ coupon)

### Logic Giỏ hàng

- Xóa các item đã mua khỏi `cart_items`

---

## 💳 Bước 5: Payment Routing (Rẽ nhánh thanh toán)

### Trường hợp COD

- `status = 'AWAITING_SHIPMENT'`
- Kết thúc luồng → trả Success

### Trường hợp Online (VNPay / MoMo)

- `status = 'PENDING_PAYMENT'`
- Gọi API sinh link thanh toán
- Trả link về Frontend để redirect
- Đặt TTL (Redis / Scheduler):
  - Sau **15 phút** không thanh toán → `CANCELLED`
  - Nhả `locked_stock`
  - Hoàn coupon

---

# ⚙️ PHẦN 2: POST-CHECKOUT

## (Vận hành & Dòng tiền)

> Các tiến trình **background job** hoặc **webhook**

---

## 📡 Bước 1: Webhook GHN (Lắng nghe trạng thái)

### API Public

```
POST /api/v1/webhooks/ghn
```

### Idempotency

- GHN có thể gửi **trùng trạng thái**
- Nếu DB đã ở trạng thái cuối → **bỏ qua**

### Mapping trạng thái

| GHN Status    | DB Status | Ghi chú                    |
| ------------- | --------- | -------------------------- |
| ready_to_pick | PICKING   |                            |
| delivered     | DELIVERED | Ghi `delivered_at = NOW()` |
| returned      | RETURNED  | Nhả `locked_stock`         |

---

## ⏳ Bước 2: Escrow 72h (Đồng hồ cát rã đông)

### Scheduler

```java
@Scheduled(cron = "0 0 * * * *") // Mỗi giờ
```

### Điều kiện

- `status = 'DELIVERED'`
- `delivered_at < NOW() - 72h`
- Không nằm trong bảng `reports` (khiếu nại)

### Kế toán kép (Double-entry)

1. `status → COMPLETED`
2. Cập nhật ví Seller:
   ```
   frozen_balance -= settlement_amount
   wallet_balance += settlement_amount
   ```
3. Insert `transactions`:
   - `ORDER_REVENUE` (+)
   - `PLATFORM_FEE` (-)
   - `TAX_FEE` (-)

---

## 🚨 Bước 3: Incident Handling (Ban Shop)

### Trigger

Admin set:

```
sellers.operational_status = 'BANNED'
```

### Hành động

Quét toàn bộ đơn:

- `PENDING_PAYMENT`
- `AWAITING_SHIPMENT`
- `SHIPPING`

### Thực thi

1. `status → CANCELLED`
2. Nếu đã thanh toán online → gọi **Refund**
3. Gọi GHN:
   ```
   /v2/switch-status/cancel
   ```
4. Notify User:
   > _"Đơn hàng bị hủy do cửa hàng vi phạm chính sách"_

---

# 🛡️ PHẦN 3: TỪ ĐIỂN EXCEPTIONS

| Exception Class                      | Trigger        | Message FE                                         | HTTP |
| ------------------------------------ | -------------- | -------------------------------------------------- | ---- |
| **CheckoutConcurrencyException**     | Oversell       | "Sản phẩm đã hết hàng trong quá trình thanh toán." | 409  |
| **GHNConnectionException**           | GHN timeout    | "Đơn vị vận chuyển đang bảo trì."                  | 503  |
| **UnsupportedDeliveryAreaException** | Ngoài vùng GHN | "Khu vực giao hàng chưa hỗ trợ."                   | 400  |
| **OrderWeightLimitException**        | > 30kg         | "Đơn hàng quá nặng."                               | 400  |
| **PaymentGatewayException**          | Gateway lỗi    | "Cổng thanh toán gián đoạn."                       | 502  |
| **InvalidWebhookSignatureException** | Webhook giả    | Log security                                       | 403  |
| **AutoRefundFailedException**        | Refund fail    | Log admin                                          | 500  |

---

# 📌 YÊU CẦU NGHIỆP VỤ

## Phương thức thanh toán

1. Online (VNPay, MoMo)
2. COD

---

## 🎟️ Logic Coupon

### Online

- Coupon **chỉ trừ** khi `payment_status = PAID`

### COD

- Trừ ngay tại checkout
- Nếu `CANCELLED` → hoàn coupon

---

## 📦 Logic Tồn kho

### Online

- `PAID` → trừ `stock_quantity`

### COD

- Checkout → tăng `locked_stock`
- `DELIVERED` → trừ tồn + giảm lock
- `CANCELLED` → nhả lock

---

## ⚙️ Yêu cầu kỹ thuật

- Transaction an toàn
- Chống overselling
- Xử lý concurrency
- Chịu được retry, webhook trùng
- Không tin dữ liệu từ Frontend
