# EasyMall — Phân tích thay đổi DB Schema (New vs V5.0)

> **Baseline cũ:** Migration V5.0  
> **Trạng thái:** 🔄 Đang cập nhật theo từng phân hệ

---

## Chú thích ký hiệu

| Ký hiệu | Ý nghĩa |
|---------|---------|
| ✅ NEW | Cột / constraint / index mới hoàn toàn |
| 🔄 CHANGED | Thay đổi kiểu dữ liệu, nullable, default... |
| ❌ REMOVED | Bị xoá khỏi schema mới |
| ⚠️ CAUTION | Cần xác nhận thêm trước khi apply |
| ✔️ UNCHANGED | Không thay đổi |

---

## PHÂN HỆ 1: IDENTITY — Định danh, Tài khoản & Phân quyền

### `roles`

| Thay đổi | Column | Cũ | Mới |
|----------|--------|----|-----|
| ❌ REMOVED | `description` | VARCHAR(255) NULL | — |
| ❌ REMOVED | `created_at` | TIMESTAMPTZ DEFAULT NOW | — |

> Schema mới rút gọn còn: `role_id`, `role_name`

---

### `users`

| Thay đổi | Column / Constraint | Cũ | Mới |
|----------|---------------------|----|-----|
| 🔄 CHANGED | `password` | `VARCHAR(255) NOT NULL DEFAULT ''` | `VARCHAR(255) NULL` |
| ✅ NEW | `email_verified_at` | — | `TIMESTAMPTZ NULL` |
| ✅ NEW | `last_login_at` | — | `TIMESTAMPTZ NULL` |
| ✅ NEW | `CONSTRAINT chk_users_password` | — | `CHECK (password IS NOT NULL OR google_account_id IS NOT NULL OR facebook_account_id IS NOT NULL)` |

> `password` chuyển sang nullable để hỗ trợ đăng nhập OAuth thuần (Google/Facebook).  
> Constraint bù đắp để đảm bảo user luôn có ít nhất 1 phương thức đăng nhập.

---

### `tokens`

| Thay đổi | Column | Cũ | Mới |
|----------|--------|----|-----|
| ⚠️ CHANGED | `refresh_token` | `VARCHAR(1000) NOT NULL` | `VARCHAR(255) NOT NULL` |
| 🔄 CHANGED | `expires_at` | `TIMESTAMPTZ NULL` | `TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP` |
| ❌ REMOVED | `device_info` | `VARCHAR(255) NULL` | — |
| ✅ NEW | `idx_tokens_user` | — | `INDEX ON tokens(user_id)` |
| ✅ NEW | `idx_tokens_refresh` | — | `INDEX ON tokens(refresh_token)` |

> ⚠️ **CAUTION:** `refresh_token` thu hẹp `1000 → 255`. JWT RT thực tế thường > 255 chars. Cần xác nhận format token đang dùng.  
> `device_info` bị xoá — tracking device có thể đã chuyển sang `device_sessions`.

---

### `addresses`

| Thay đổi | Column | Cũ | Mới |
|----------|--------|----|-----|
| ❌ REMOVED | `province_name` | `VARCHAR(100) NULL` (V4.8) | — |
| ❌ REMOVED | `district_name` | `VARCHAR(100) NULL` (V4.8) | — |
| ❌ REMOVED | `ward_name` | `VARCHAR(100) NULL` (V4.8) | — |

> Display names của GHN bị xoá khỏi DB — sẽ resolve ở tầng service/response khi cần.

---

### `permissions` & `role_permissions`

✔️ **Không thay đổi**

---

### ❓ Open Questions — Phân hệ 1

1. `refresh_token VARCHAR(255)` — đủ dài cho JWT RT hiện tại? (thường ~300–500 chars)
2. `device_info` bị xoá — chức năng tracking đã chuyển hoàn toàn sang `device_sessions`?
3. `province_name / district_name / ward_name` bỏ — GHN display names resolve ở layer nào?
4. `roles.description` & `roles.created_at` bỏ — có còn dùng trong UI/Admin không?

---

## PHÂN HỆ 2: CATALOG — Sản phẩm & Quản lý kho

### `categories`

| Thay đổi | Column | Cũ | Mới |
|----------|--------|----|-----|
| ❌ REMOVED | `target_demographic` | `SMALLINT NULL` | — |
| ❌ REMOVED | `category_type` | `VARCHAR(30) DEFAULT 'STANDARD'` | — |
| ✅ NEW | `updated_at` | — | `TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP` |
| ✅ NEW | `trg_categories_updated_at` | — | Trigger auto-update `updated_at` |

> `target_demographic` và `category_type` bị xoá — phân loại theo nhân khẩu học được đơn giản hoá.

---

### `products`

| Thay đổi | Column | Cũ | Mới |
|----------|--------|----|-----|
| 🔄 CHANGED | `created_at` | `TIMESTAMP` (no tz) | `TIMESTAMPTZ` |
| ✅ NEW | `updated_at` | — | `TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP` |
| ✅ NEW | `trg_products_updated_at` | — | Trigger auto-update `updated_at` |
| ✅ NEW | `min_price` | — | `NUMERIC(15,2) DEFAULT 0.00` |
| ✅ NEW | `max_price` | — | `NUMERIC(15,2) DEFAULT 0.00` |
| ✅ NEW | `view_count` | — | `INT DEFAULT 0` |
| ✅ NEW | `sold_count` | — | `INT DEFAULT 0` |
| ✅ NEW | `rating_avg` | — | `DECIMAL(3,2) DEFAULT 0.00` |
| ✅ NEW | `rating_count` | — | `INT DEFAULT 0` |
| ✅ NEW | `popularity_score` | — | `DECIMAL(10,4) DEFAULT 0.00` |

> `min_price` / `max_price` — denormalized từ `product_variants`, cập nhật khi variant thay đổi.  
> `view_count`, `sold_count`, `rating_avg`, `rating_count`, `popularity_score` — denormalized stats phục vụ listing/sort không cần JOIN.

---

### `product_variants`

| Thay đổi | Column / Constraint | Cũ | Mới |
|----------|---------------------|----|-----|
| ✅ NEW | `is_unlimited` | — | `BOOLEAN DEFAULT FALSE` |
| ✅ NEW | `updated_at` | — | `TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP` |
| ✅ NEW | `trg_product_variants_updated_at` | — | Trigger auto-update `updated_at` |
| ✅ NEW | `CONSTRAINT chk_stock_locked` | — | `CHECK (stock_quantity >= locked_stock)` |
| 🔄 CHANGED | `chk_stock_valid` / `chk_locked_non_negative` | 2 constraint ALTER riêng | Inline trong column definition |

> `is_unlimited = TRUE` → variant không giới hạn stock (digital goods / pre-order).  
> `chk_stock_locked`: đảm bảo `locked_stock` không vượt quá `stock_quantity` hiện có.

---

### `product_images`

✔️ **Không thay đổi**

---

### `sliders`

✔️ **Không thay đổi**

---

### ✅ Design Decisions — Phân hệ 2 (Đã xác nhận)

| # | Quyết định | Chi tiết |
|---|-----------|---------|
| 1 | `min_price` / `max_price` | **DB Trigger** — tự động cập nhật khi variant INSERT/UPDATE/DELETE |
| 2 | `rating_avg` / `rating_count` | Cập nhật khi review được **APPROVED** (không cập nhật khi tạo) |
| 3 | `view_count` | Tăng mỗi lần gọi **product detail API** |
| 3 | `sold_count` | Tăng khi **order HOÀN THÀNH** (order có chứa product này) |
| 4 | `popularity_score` | **Cron job** định kỳ — công thức: `(0.5 × view_count) + (0.3 × sold_count) + (0.2 × rating_avg × log(rating_count))` |
| 5 | `target_demographic` bỏ | Dùng `products.target_gender` thay thế cho filter/recommendation |


---

## PHÂN HỆ 3: CART — Giỏ hàng & Mua sắm

### `carts` — ✔️ Không đổi

| Column | Giá trị cũ (V2.8) | Giá trị mới | Nhận xét |
|--------|------------------|-----------|---------|
| `cart_id` | BIGINT IDENTITY PK | ✔️ | Giữ nguyên |
| `is_active` | BOOLEAN DEFAULT TRUE | ✔️ | Giữ nguyên |
| `user_id` | BIGINT FK → users CASCADE | ✔️ | Giữ nguyên |

> Schema mới giữ nguyên hoàn toàn. **Không cần migration.**

---

### `cart_items` — 🔄 Có thay đổi nhỏ

| Column / Constraint | Giá trị cũ (V2.8) | Giá trị mới | Trạng thái |
|---------------------|------------------|-----------|-----------|
| `cart_item_id` | BIGINT IDENTITY PK | ✔️ | Giữ nguyên |
| `cart_id` | BIGINT FK CASCADE | ✔️ | Giữ nguyên |
| `variant_id` | BIGINT FK CASCADE | ✔️ | Giữ nguyên |
| `quantity` | INT NOT NULL CHECK (quantity > 0) | ✔️ | Giữ nguyên |
| `total_money` | **DECIMAL(12,2)** NOT NULL | **NUMERIC(15,2)** | 🔄 CHANGED |
| `note` | VARCHAR(200) NULL | ✔️ | Giữ nguyên |
| `uq_cart_items_cart_variant` | _chưa có_ | UNIQUE (cart_id, variant_id) | ✅ NEW |

#### So sánh chi tiết

**`total_money` precision:**
- Cũ: `DECIMAL(12,2)` — tối đa 999.999.999.99
- Mới: `NUMERIC(15,2)` — tối đa 9.999.999.999.999.99
- → Entity hiện dùng `precision=12` — cần sửa thành `precision=15`

**`uq_cart_items_cart_variant` UNIQUE constraint:**
- Ngăn user thêm cùng 1 variant vào giỏ 2 lần — hợp lý về nghiệp vụ
- Hiện chưa có trong DB, cần thêm qua migration
- Entity hiện chưa có `@Table(uniqueConstraints = ...)` — cần bổ sung

---

### ✅ Design Decisions — Phân hệ 3

| # | Quyết định | Chi tiết |
|---|-----------|----------|
| 1 | `carts` | Giữ nguyên, không migration |
| 2 | `total_money` precision | Tăng `DECIMAL(12,2)` → `NUMERIC(15,2)` — đồng bộ với giá variant |
| 3 | Unique constraint | Thêm `UNIQUE (cart_id, variant_id)` và `@Table` annotation — tránh duplicate item logic ở application layer |

### Các file cần thay đổi

| File | Loại | Nội dung |
|------|-------|---------|
| `V5.9__alter_cart_items.sql` | Migration | (1) ALTER `total_money` precision; (2) ADD UNIQUE constraint |
| `CartItemEntity.java` | Entity | Sửa `precision=12` → `15`; thêm `@Table(uniqueConstraints=...)` |

## PHÂN HỆ 4: TRANSACTIONS — Đơn hàng, Giao dịch & Vận chuyển

### `orders` — 🔄 Có nhiều thay đổi

| Column | Cũ (V3.2) | Mới | Trạng thái |
|--------|-----------|------|-----------|
| `order_date` | **DATE** DEFAULT CURRENT_DATE | **TIMESTAMPTZ** DEFAULT NOW | 🔄 CHANGED |
| `shipping_date` | **DATE** NULL | **TIMESTAMPTZ** NULL | 🔄 CHANGED |
| `tracking_number` | VARCHAR(100) **NOT NULL** DEFAULT '' | VARCHAR(100) NULL **DEFAULT NULL** | 🔄 CHANGED |
| `device_session_id` | BIGINT FK → device_sessions | _bỏ_ | ❌ REMOVED |
| `shop_discount_amount` | NUMERIC(15,2) DEFAULT 0 | _bỏ_ | ❌ REMOVED |
| `updated_at` | _chưa có_ | TIMESTAMPTZ DEFAULT NOW + trigger | ✅ NEW |

#### Chi tiết thay đổi quan trọng

**`order_date` / `shipping_date` DATE → TIMESTAMPTZ:**
- Ghi nhận chính xác thời điểm (giờ:phút:giây) thay vì chỉ ngày
- Entity hiện dùng `LocalDate` — cần đổi sang `OffsetDateTime`

**`tracking_number` NOT NULL → NULL:**
- Hợp lý vì đơn mới tạo chưa có mã vận đơn
- Entity hiện có `nullable = false` + `@Builder.Default = ""` — cần sửa thành nullable

**`device_session_id` bỏ:**
- Loại bỏ phụ thuộc vào bảng `device_sessions` — đơn giản hóa model
- Entity hiện có field `deviceSession` và FK join — cần xóa

**`shop_discount_amount` bỏ:**
- Schema mới không có cột này — loại bỏ voucher giảm tiền hàng tại Order level
- Entity hiện có field `shopDiscountAmount` — cần xóa

**`updated_at` mới + trigger:**
- Auto-set qua `trigger_set_timestamp()` như các bảng khác

---

### `order_details` — 🔄 CHANGED precision

| Column | Cũ (V3.3) | Mới | Trạng thái |
|--------|-----------|------|-----------|
| `order_detail_price` | **DECIMAL(12,2)** | **NUMERIC(15,2)** | 🔄 CHANGED |
| `total_money` | **DECIMAL(12,2)** | **NUMERIC(15,2)** | 🔄 CHANGED |

> Entity hiện dùng `precision=12` cho cả 2 cột — cần sửa thành `precision=15`.

---

### `payments` — ✅ BRAND NEW (chưa có trong cây hiện tại)

Bảng thanh toán hoàn toàn mới, tách riêng khỏi `orders`.

| Column | Kiểu | Ghi chú |
|--------|-------|---------|
| `payment_id` | BIGINT IDENTITY PK | |
| `order_id` | BIGINT FK → orders CASCADE | |
| `payment_method` | SMALLINT | 0: COD, 1: VNPAY, 2: MOMO |
| `amount` | NUMERIC(15,2) | Số tiền thanh toán |
| `transaction_id` | VARCHAR(255) NULL | Mã giao dịch từ cổng thanh toán |
| `payment_status` | VARCHAR(50) DEFAULT 'PENDING' | PENDING / PAID / FAILED / REFUNDED |
| `paid_at` | TIMESTAMPTZ NULL | Thời điểm thanh toán thành công |
| `created_at` | TIMESTAMPTZ DEFAULT NOW | |

> Cần tạo **`PaymentEntity`** và **migration mới**.

---

### `notifications` — ✅ BRAND NEW (chưa có trong cây hiện tại)

Bảng thông báo in-app hoàn toàn mới.

| Column | Kiểu | Ghi chú |
|--------|-------|---------|
| `notification_id` | BIGINT IDENTITY PK | |
| `user_id` | BIGINT FK → users CASCADE | |
| `title` | VARCHAR(200) | Tiêu đề thông báo |
| `content` | TEXT | Nội dung |
| `notification_type` | VARCHAR(50) | ORDER_UPDATE, SYSTEM... |
| `reference_id` | BIGINT NULL | order_id / product_id tuỳ type |
| `is_read` | BOOLEAN DEFAULT FALSE | |
| `created_at` | TIMESTAMPTZ DEFAULT NOW | |
| `idx_notifications_user_read` | INDEX ON (user_id, is_read) | Tối ưu query đọc thông báo chưa đọc |

> Cần tạo **`NotificationEntity`** và **migration mới**.

---

### ✅ Design Decisions — Phân hệ 4

| # | Quyết định | Chi tiết |
|---|-----------|----------|
| 1 | `order_date` / `shipping_date` | `LocalDate` → `OffsetDateTime` — nhất quán TIMESTAMPTZ toàn hệ thống |
| 2 | `tracking_number` | Bỏ `NOT NULL` + default `""` — nullable hợp lý hơn |
| 3 | `device_session_id` | Xóa FK và field — đơn giản hóa entity |
| 4 | `shop_discount_amount` | Xóa — không còn trong schema mới |
| 5 | `updated_at` | Thêm — đồng bộ pattern với các bảng khác |
| 6 | `payments` | Tạo mới — tách lịch sử thanh toán khỏi đơn hàng |
| 7 | `notifications` | Tạo mới — in-app notification với index tối ưu |

### Các file cần thay đổi

| File | Loại | Nội dung |
|------|-------|---------|
| `V5.10__alter_orders.sql` | Migration | DATE→TIMESTAMPTZ; DROP device_session_id, shop_discount_amount; ADD updated_at+trigger; ALTER tracking_number NULL |
| `V5.11__alter_order_details.sql` | Migration | ALTER precision cả 2 cột DECIMAL(12)→NUMERIC(15) |
| `V5.12__create_payments.sql` | Migration | CREATE TABLE payments |
| `V5.13__create_notifications.sql` | Migration | CREATE TABLE notifications + index |
| `OrderEntity.java` | Entity | LocalDate→OffsetDateTime; xóa deviceSession, shopDiscountAmount; thêm updatedAt |
| `OrderDetailEntity.java` | Entity | precision 12→ 15 cả 2 cột |
| `PaymentEntity.java` | Entity | **[MỜI]** — toàn bộ |
| `NotificationEntity.java` | Entity | **[MỜI]** — toàn bộ |

---

## PHÂN HỆ 5: COUPONS — Khuyến mãi & Chiết khấu

### `coupons` — ✔️ Không đổi

So sánh schema mới với V3.1:

| Column / Constraint | Cũ (V3.1) | Mới | Trạng thái |
|---------------------|-----------|------|------------|
| `coupon_id` | BIGINT ALWAYS AS IDENTITY PK | ✔️ | Giữ nguyên |
| `code` | VARCHAR(50) NOT NULL UNIQUE | ✔️ | Giữ nguyên |
| `description` | VARCHAR(255) NULL | ✔️ | Giữ nguyên |
| `discount_type` | VARCHAR(20) NOT NULL | ✔️ | Giữ nguyên |
| `discount_value` | NUMERIC(15,2) NOT NULL | ✔️ | Giữ nguyên |
| `max_discount_amount` | NUMERIC(15,2) NULL | ✔️ | Giữ nguyên |
| `min_order_amount` | NUMERIC(15,2) DEFAULT 0 | ✔️ | Giữ nguyên |
| `max_usage` | INT DEFAULT 1000 | ✔️ | Giữ nguyên |
| `user_usage_limit` | INT DEFAULT 1 | ✔️ | Giữ nguyên |
| `start_date` | TIMESTAMPTZ NOT NULL | ✔️ | Giữ nguyên |
| `end_date` | TIMESTAMPTZ NOT NULL | ✔️ | Giữ nguyên |
| `is_active` | BOOLEAN DEFAULT TRUE | ✔️ | Giữ nguyên |
| `applicable_conditions` | JSONB DEFAULT '{}' | ✔️ | Giữ nguyên |
| `coupon_type` | VARCHAR(30) NOT NULL DEFAULT 'SHOP_VOUCHER' | ✔️ | Giữ nguyên |
| `created_at` | TIMESTAMPTZ DEFAULT NOW | ✔️ | Giữ nguyên |

> Schema mới khớp hoàn toàn với V3.1. **Không cần migration.**

#### Ghi chú entity

`CouponEntity` dùng `@Enumerated(EnumType.STRING)` cho `discountType` và `couponType` — khớp với VARCHAR trong DB. `applicableConditions` lưu dạng `String` (JSON raw) phù hợp cho JSONB linh hoạt.

---

### `coupon_usages` — ✔️ Không đổi

So sánh schema mới với V3.4:

| Column / Constraint | Cũ (V3.4) | Mới | Trạng thái |
|---------------------|----------|-----|-----------|
| `coupon_usage_id` | BIGINT IDENTITY PK | ✔️ | Giữ nguyên |
| `user_id` | BIGINT FK → users CASCADE | ✔️ | Giữ nguyên |
| `coupon_id` | BIGINT FK → coupons CASCADE | ✔️ | Giữ nguyên |
| `order_id` | BIGINT FK → orders CASCADE | ✔️ | Giữ nguyên |
| `used_at` | TIMESTAMPTZ DEFAULT NOW NOT NULL | ✔️ | Giữ nguyên |
| `uq_coupon_usage` | UNIQUE(user_id, coupon_id, order_id) | ✔️ | Giữ nguyên |

> Schema mới khớp hoàn toàn với V3.4. **Không cần migration.**

#### Ghi chú về `order_id` trong Entity

`CouponUsageEntity` lưu `order_id` dưới dạng `Long` thô (không `@ManyToOne`) — đây là **design có chủ ý** để tránh circular dependency với `OrderEntity`. Giữ nguyên.

---

### ✅ Design Decisions — Phân hệ 5

| # | Quyết định | Chi tiết |
|---|-----------|------|
| 1 | `coupons` | Giữ nguyên — khớp hoàn toàn V3.1 |
| 2 | `coupon_usages` | Giữ nguyên — schema đã đúng từ V3.4 |
| 3 | `orderId` raw field | Giữ kiểu `Long` thay vì FK entity — tránh circular dependency |

> **Không cần migration mới cho PH5.**

---

## PHÂN HỆ 6: ANALYTICS — Đánh giá & Kho số liệu

### `reviews` — ⚠️ 1 delta

So sánh schema mới với V3.8:

| Column / Constraint | Cũ (V3.8) | Mới | Trạng thái |
|---------------------|-----------|------|------------|
| `review_id` | BIGINT IDENTITY PK | ✔️ | Giữ nguyên |
| `user_id` | BIGINT FK → users CASCADE | ✔️ | Giữ nguyên |
| `product_id` | BIGINT FK → products CASCADE | ✔️ | Giữ nguyên |
| `order_id` | BIGINT FK → orders CASCADE | ✔️ | Giữ nguyên |
| `rating` | INT NOT NULL CHECK (1–5) | ✔️ | Giữ nguyên |
| `comment` | TEXT NULL | ✔️ | Giữ nguyên |
| `review_status` | VARCHAR(20) DEFAULT 'PENDING' NOT NULL | ✔️ | Giữ nguyên |
| `reply_at` | TIMESTAMPTZ NULL | ✔️ | Giữ nguyên |
| `created_at` | TIMESTAMPTZ DEFAULT NOW NOT NULL | ✔️ | Giữ nguyên |
| `updated_at` | TIMESTAMPTZ DEFAULT NOW NOT NULL | ✔️ | Giữ nguyên |
| `trg_reviews_updated_at` | trigger | ✔️ | Giữ nguyên |
| `uq_reviews_user_product_order` | _chưa có_ | UNIQUE(user_id, product_id, order_id) | ✅ NEW |

> Delta duy nhất: schema mới thêm **UNIQUE constraint** đảm bảo mỗi user chỉ review 1 lần cho mỗi cặp (user, product, order).

#### Ghi chú entity

`ReviewEntity` có 2 điểm khác với DDL mới:
- `order_id` đặt `ON DELETE CASCADE` trong DDL, nhưng entity dùng `ON DELETE SET NULL` (theo comment intent). Cần xác nhận lại với owner.
- **Đã thêm** `@Table(uniqueConstraints)` vào `ReviewEntity` để khớp với migration.

---

### `review_images` — ✔️ Không đổi

Schema mới khớp hoàn toàn với V3.8. **Không cần migration.**

---

### `inventory_transactions` — ✔️ Không đổi

So sánh schema mới với V2.5:

| Column | Cũ (V2.5) | Mới | Trạng thái |
|--------|-----------|------|-----------|
| `transaction_id` | BIGINT IDENTITY PK | ✔️ | Giữ nguyên |
| `variant_id` | BIGINT NOT NULL FK → product_variants CASCADE | ✔️ | Giữ nguyên |
| `quantity_change` | INT NOT NULL | ✔️ | Giữ nguyên |
| `transaction_type` | VARCHAR(50) NOT NULL | ✔️ | Giữ nguyên |
| `reference_id` | BIGINT NULL | ✔️ | Giữ nguyên |
| `created_at` | TIMESTAMPTZ DEFAULT NOW | ✔️ | Giữ nguyên |

> Schema mới khớp hoàn toàn với V2.5. **Không cần migration.**

#### Ghi chú entity

`InventoryTransactionEntity` dùng `@PrePersist` để set `createdAt` thay vì `@CreationTimestamp` — cả 2 cách đều đúng, giữ nguyên.

---

### `wishlists` — ✔️ Không đổi

So sánh schema mới với V4.0:

| Column / Constraint | Cũ (V4.0) | Mới | Trạng thái |
|---------------------|-----------|------|------------|
| `wishlist_id` | BIGINT IDENTITY PK | ✔️ | Giữ nguyên |
| `user_id` | BIGINT FK → users CASCADE | ✔️ | Giữ nguyên |
| `product_id` | BIGINT FK → products CASCADE | ✔️ | Giữ nguyên |
| `created_at` | TIMESTAMPTZ DEFAULT NOW NOT NULL | ✔️ | Giữ nguyên |
| `updated_at` | TIMESTAMPTZ DEFAULT NOW NOT NULL | ✔️ | Giữ nguyên |
| `trg_wishlists_updated_at` | trigger | ✔️ | Giữ nguyên |

> Schema mới khớp hoàn toàn với V4.0. **Không cần migration.**

#### Ghi chú entity

`WishlistEntity` có `uq_wishlists_user_product` trong `@Table(uniqueConstraints)` nhưng V4.0 baseline chưa có constraint này trong DB. Đây là delta riêng biệt không thuộc PH6 — **ghi nhận để xử lí sau**.

---

### ✅ Design Decisions — Phân hệ 6

| # | Quyết định | Chi tiết |
|---|-----------|------|
| 1 | `reviews` UNIQUE | Thêm constraint — tạo V5.14 + sửa entity |
| 2 | `review_images` | Giữ nguyên |
| 3 | `inventory_transactions` | Giữ nguyên |
| 4 | `wishlists` | Giữ nguyên |
| 5 | `order_id` ON DELETE | Cần xác nhận CASCADE vs SET NULL giữa DDL mới và entity |

#### Files đã tạo / sửa

| File | Loại | Nội dung |
|------|-------|----------|
| `V5.14__alter_reviews_add_unique.sql` | Migration | ADD CONSTRAINT uq\_reviews\_user\_product\_order UNIQUE |
| `ReviewEntity.java` | Entity | Thêm `@Table(uniqueConstraints)` |

---

## PHÂN HỆ 7: AI INTEGRATION — Dữ liệu AI

### `user_behaviors` — ⚠️ Nhiều delta lớn

So sánh schema mới với V4.1:

| Column / Đặc tính | Cũ (V4.1) | Mới | Trạng thái |
|---------------------|------------|------|------------|
| `user_behavior_id` | BIGINT **ALWAYS** AS IDENTITY **PK** | BIGINT **BY DEFAULT** AS IDENTITY *(không PK riêng)* | 🔄 CHANGED |
| `duration_seconds` | _không có_ (nằm trong context_data JSONB) | INT NULL | ✅ NEW |
| `source` | _không có_ | VARCHAR(50) NULL | ✅ NEW |
| `variant_id` | BIGINT FK → product_variants | BIGINT NULL *(không có FK constraint)* | 🔄 CHANGED |
| `created_at` | TIMESTAMPTZ DEFAULT NOW | TIMESTAMPTZ NOT NULL DEFAULT NOW | 🔄 CHANGED |
| `idx_behavior_user` | ON (user_id) | ON **(user_id, created_at DESC)** | 🔄 CHANGED |
| `idx_behavior_product` | ON (product_id) | ON **(product_id, created_at DESC)** | 🔄 CHANGED |
| `idx_behavior_category` | ON (category_id) | _bị xoá_ | ❌ REMOVED |
| `idx_behavior_action_time` | _không có_ | ON (action_type, created_at DESC) | ✅ NEW |
| `idx_behavior_context` | GIN(context_data) | GIN(context_data) | ✔️ Giữ |
| **PRIMARY KEY** | `user_behavior_id` (single) | **(user_behavior_id, created_at)** (composite) | 🔄 CHANGED |
| **PARTITION** | _không có_ | PARTITION BY RANGE(created_at) + 3 partition tables | ✅ NEW |

> Đây là **thay đổi kiến trúc lớn nhất** của toàn bộ schema mới: bảng chuyển sang **Table Partitioning** theo `created_at`.

#### Phân tích Partitioning

```sql
-- Composite PK bắt buộc do PostgreSQL yêu cầu PK phải bao gồm partition key:
PRIMARY KEY (user_behavior_id, created_at)

-- 3 partitions hiện tại (cần tạo thêm hàng tháng):
user_behaviors_y2026m05  [2026-05-01, 2026-06-01)
user_behaviors_y2026m06  [2026-06-01, 2026-07-01)
user_behaviors_y2026m07  [2026-07-01, 2026-08-01)
```

#### Tác động lên Entity

`UserBehaviorEntity` hiện có các vấn đề sau:

| Vấn đề | Mô tả |
|---------|--------|
| `@Id` đơn lẻ | Entity chỉ map `user_behavior_id` làm PK — JPA không hỗ trợ composite PK trong table partition trực tiếp |
| Thiếu `duration_seconds` | Cần thêm field `Integer durationSeconds` |
| Thiếu `source` | Cần thêm field `String source` |
| `variant_id` vẫn FK đầy đủ | Schema mới không có FK constraint — có thể giữ `@ManyToOne` nhưng cần biết |
| Partitioned table & JPA | JPA `save()` hoạt động bình thường với partitioned table, nhưng **không thể JOIN cross-partition** hiệu quả |

> ⚠️ **Lưu ý quan trọng:** Partitioned table cần **tạo thêm partition mật tháng** — nên có batch job/cron job tự động tạo partition tương lai.

---

### `product_similarities` — ✅ NEW TABLE

_Không có trong baseline (chưa tồn tại migration cũ)_

| Column | Kiểu | Đặc tính |
|--------|-------|----------|
| `product_id` | BIGINT FK → products CASCADE | Part of PK |
| `similar_product_id` | BIGINT FK → products CASCADE | Part of PK |
| `score` | DOUBLE PRECISION NOT NULL | Điểm tương đồng [0.0–1.0] |
| `similarity_type` | VARCHAR(50) DEFAULT 'CONTENT\_BASED' NOT NULL | Part of PK |
| **PRIMARY KEY** | **(product_id, similar_product_id, similarity_type)** | Composite |

> **Mục đích:** Lưu kết quả thi thinhưtật Collaborative Filtering và Content-Based Filtering batch (không real-time). PK composite bảo đảm 1 cặp (product, similar, type) chỉ có 1 score.

---

### `user_recommendations` — ✅ NEW TABLE

_Không có trong baseline (chưa tồn tại migration cũ)_

| Column | Kiểu | Đặc tính |
|--------|-------|----------|
| `user_id` | BIGINT FK → users CASCADE | Part of PK |
| `product_id` | BIGINT FK → products CASCADE | Part of PK |
| `recommendation_score` | DOUBLE PRECISION NOT NULL | Score gợi ý |
| `algorithm` | VARCHAR(50) DEFAULT 'HYBRID' NOT NULL | Part of PK |
| `created_at` | TIMESTAMPTZ DEFAULT NOW | Thời điểm tính score |
| **PRIMARY KEY** | **(user_id, product_id, algorithm)** | Composite |

> **Mục đích:** Cache kết quả Recommendation Engine theo từng algorithm. Batch job chạy định kỳ để cập nhật bảng này, API chỉ query.

---

### `product_associations` — ✅ NEW TABLE

_Không có trong baseline (chưa tồn tại migration cũ)_

| Column | Kiểu | Đặc tính |
|--------|-------|----------|
| `product_id` | BIGINT FK → products CASCADE | Part of PK |
| `related_product_id` | BIGINT FK → products CASCADE | Part of PK |
| `confidence` | DOUBLE PRECISION NOT NULL | Market Basket Analysis metric |
| `lift` | DOUBLE PRECISION NOT NULL | Market Basket Analysis metric |
| **PRIMARY KEY** | **(product_id, related_product_id)** | Composite |

> **Mục đích:** Lưu kết quả **Apriori / Association Rule Mining** — "Khách mua A thường mua B" (sản phẩm có liên quan). Khác `product_similarities` ở chỗ dùng confidence/lift thay vì similarity score.

---

### ✅ Design Decisions — Phân hệ 7

| # | Quyết định | Chi tiết | Priority |
|---|-----------|------|----------|
| 1 | `user_behaviors` Partitioning | Thêm PARTITION BY RANGE + composite PK | 🔴 High |
| 2 | Thêm `duration_seconds`, `source` | 2 cột mới trong `user_behaviors` | 🔴 High |
| 3 | `idx_behavior_category` | Xóa index cũ, thay bằng index mới có `created_at` | 🔴 High |
| 4 | `product_similarities` | NEW TABLE — chưa có entity | 🟡 Medium |
| 5 | `user_recommendations` | NEW TABLE — chưa có entity | 🟡 Medium |
| 6 | `product_associations` | NEW TABLE — chưa có entity | 🟡 Medium |
| 7 | Partition maintenance | Cần batch job tạo partition mới hàng tháng | 🔴 High |

> ⚠️ **Chưa implement** — PH7 có mức độ phức tạp cao nhất do Partitioning đòi hỏi thay đổi kiến trúc entity (composite PK không native trong Spring Data JPA).

## 📊 Tổng kết toàn bộ thay đổi

| Phân hệ | Bảng | NEW cols | CHANGED cols | REMOVED cols | Status |
|---------|------|----------|--------------|--------------|--------|
| PH1 | `roles` | 0 | 0 | 2 | ✅ Đã implement |
| PH1 | `users` | 3 | 1 | 0 | ✅ Đã implement |
| PH1 | `tokens` | 2 indexes | 1 | 1 | ✅ Đã implement |
| PH1 | `addresses` | 0 | 0 | 3 | ✅ Đã implement |
| PH1 | `permissions` | — | — | — | ✔️ Không đổi |
| PH1 | `role_permissions` | — | — | — | ✔️ Không đổi |
| PH2 | `categories` | 1+trigger | 0 | 2 | ✅ Đã implement |
| PH2 | `products` | 8+trigger | 1 | 0 | ✅ Đã implement |
| PH2 | `product_variants` | 2+trigger+constraint | 0 | 0 | ✅ Đã implement |
| PH2 | `product_images` | — | — | — | ✔️ Không đổi |
| PH2 | `sliders` | — | — | — | ✔️ Không đổi |
| PH3 | `carts` | 0 | 0 | 0 | ✔️ Không đổi |
| PH3 | `cart_items` | 1 constraint | 1 (precision) | 0 | ✅ Đã implement |
| PH4 | `orders` | 1 (updated_at) | 3 | 2 | ✅ Đã implement |
| PH4 | `order_details` | 0 | 2 (precision) | 0 | ✅ Đã implement |
| PH4 | `payments` | NEW TABLE | — | — | ✅ Đã implement (entity + migration) |
| PH4 | `notifications` | NEW TABLE | — | — | ✅ Đã implement (entity + migration) |
| PH5 | `coupons` | 0 | 0 | 0 | ✔️ Không đổi |
| PH5 | `coupon_usages` | 0 | 0 | 0 | ✔️ Không đổi |
| PH6 | `reviews` | 1 constraint | 0 | 0 | ✅ Đã implement (V5.14) |
| PH6 | `review_images` | 0 | 0 | 0 | ✔️ Không đổi |
| PH6 | `inventory_transactions` | 0 | 0 | 0 | ✔️ Không đổi |
| PH6 | `wishlists` | 0 | 0 | 0 | ✔️ Không đổi |
| PH7 | `user_behaviors` | 2 cols + partition + index | 4 | 1 index | ⏳ Chưa implement |
| PH7 | `product_similarities` | NEW TABLE | — | — | ⏳ Chưa implement |
| PH7 | `user_recommendations` | NEW TABLE | — | — | ⏳ Chưa implement |
| PH7 | `product_associations` | NEW TABLE | — | — | ⏳ Chưa implement |
| PH8–N | ... | — | — | — | ⏳ Chờ |
