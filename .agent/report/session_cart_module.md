# Session Report — Cart Module

**Date:** 2026-06-22
**Module:** Cart · CartItem
**Status:** ✅ COMPLETED — `mvnw compile` BUILD SUCCESS

---

## 1. Executive Summary

Phiên làm việc này implement toàn bộ module **Cart** theo kiến trúc singleton cart per user. Mỗi user chỉ sở hữu duy nhất 1 giỏ hàng, được tạo tự động khi truy cập lần đầu. Hệ thống được tích hợp đầy đủ bảo vệ overselling thông qua DB constraint, kiểm tra tồn kho thực (Available Stock), và phân quyền chuẩn `resource:action`.

**Key Achievements:**
- Implement đầy đủ CRUD cho Cart Items (add, update, remove, clear).
- Áp dụng **singleton cart pattern**: cart được tạo lazy (getOrCreate) khi user lần đầu gọi API.
- Tích hợp **Available Stock formula**: `available = stock_quantity - locked_stock` (khi `stock_quantity != -1`).
- Kiểm tra `max_order_quantity` (= 0 nghĩa là không giới hạn).
- Bảo vệ overselling qua **DB constraint** (`chk_stock_valid`) thay vì locking RAM.
- Fix constraint `chk_stock_valid` để hỗ trợ `stock_quantity = -1` (vô hạn).
- Sửa lỗi convention permission từ `CART_VIEW` → `cart:view` cho đồng nhất toàn project.
- Lookup user bằng email từ JWT subject (`authentication.getName()`).

---

## 2. Database Migrations

| File | Nội dung |
|---|---|
| `V2.8__create_cart_and_cart_items_table.sql` | Bảng `carts` và `cart_items` (do user tạo sẵn) |
| `V2.9__fix_stock_constraint_and_seed_cart_permissions.sql` | Fix constraint `chk_stock_valid`, seed permissions `cart:view` + `cart:manage` vào role `USER` |

> **Note:** V2.8 được user tạo trực tiếp — AI chỉ tham chiếu schema. V2.9 do AI tạo sau khi phát hiện constraint bug.

---

## 3. Các Files Đã Tạo / Chỉnh Sửa

### 3.1 New Files

| Layer | File | Mô tả |
|---|---|---|
| **Entity** | `entity/CartEntity.java` | JPA entity cho bảng `carts`. `@OneToOne(UserEntity)`, `@OneToMany(CartItemEntity)`. |
| **Entity** | `entity/CartItemEntity.java` | Item entity với `@ManyToOne(CartEntity)` và `@ManyToOne(ProductVariantEntity)`. |
| **Repository** | `repository/CartRepository.java` | `findByUser(UserEntity)`, `findByUser_Email(String)` |
| **Repository** | `repository/CartItemRepository.java` | `findByCartAndVariant_VariantId(CartEntity, Long)`, `findByCart(CartEntity)` |
| **DTO Request** | `dtos/request/cart/CartItemRequest.java` | `variantId` + `quantity` (min = 1) |
| **DTO Response** | `dtos/response/cart/CartResponse.java` | `cartId`, `userId`, `items`, `totalPrice` |
| **DTO Response** | `dtos/response/cart/CartItemResponse.java` | `variantId`, `productName`, `skuCode`, `quantity`, `price`, `subtotal` |
| **Mapper** | `mapper/CartMapper.java` | MapStruct interface — `CartItemEntity` → `CartItemResponse` |
| **Service** | `service/cart/CartService.java` | Interface: `getCart`, `addItem`, `updateItem`, `removeItem`, `clearCart` |
| **Service Impl** | `service/cart/impl/CartServiceImpl.java` | Business logic đầy đủ + helper methods |
| **Controller** | `controller/CartController.java` | 5 endpoints RESTful, trả về `ApiResponse<T>` |
| **Migration** | `db/migration/V2.9__fix_stock_constraint_and_seed_cart_permissions.sql` | Fix constraint + seed permissions |

### 3.2 Modified Files

| File | Thay đổi |
|---|---|
| `controller/CartController.java` | Fix `@PreAuthorize` từ `CART_VIEW`/`CART_MANAGE` → `cart:view`/`cart:manage` |
| `service/cart/impl/CartServiceImpl.java` | Remove unused variable `stock` (lint cleanup) |
| `db/migration/V2.9__...sql` | Fix permission key từ `CART_VIEW` → `cart:view` |

---

## 4. API Endpoints

**Base URL:** `/api/v1/carts`

| Method | Path | Permission | Mô tả |
|---|---|---|---|
| `GET` | `/me` | `cart:view` | Lấy giỏ hàng hiện tại của user |
| `POST` | `/me/items` | `cart:manage` | Thêm variant vào giỏ (hoặc tăng số lượng nếu đã có) |
| `PUT` | `/me/items/{variantId}` | `cart:manage` | Cập nhật số lượng của 1 item |
| `DELETE` | `/me/items/{variantId}` | `cart:manage` | Xóa 1 item khỏi giỏ |
| `DELETE` | `/me` | `cart:manage` | Xóa toàn bộ giỏ hàng |

---

## 5. Business Logic Chi Tiết

### 5.1 User Identity Resolution

JWT subject lưu `email` (không phải userId). Controller extract email bằng `authentication.getName()` rồi truyền vào service. Service lookup UserEntity bằng `userRepository.findByEmail(email)`.

```java
// Controller
String email = authentication.getName();
cartService.addItem(email, request);

// Service
UserEntity user = userRepository.findByEmail(email)
    .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
```

### 5.2 Singleton Cart Pattern

```java
private CartEntity getOrCreateCart(UserEntity user) {
    return cartRepository.findByUser(user)
            .orElseGet(() -> {
                CartEntity newCart = CartEntity.builder().user(user).build();
                return cartRepository.save(newCart);
            });
}
```

### 5.3 Available Stock Formula

```
available_stock = stock_quantity - locked_stock   (khi stock_quantity != -1)
available_stock = ∞                                (khi stock_quantity == -1)
```

- `stock_quantity = -1` → vô hạn tồn kho, bỏ qua mọi kiểm tra số lượng.
- `max_order_quantity = 0` → không giới hạn số lượng mỗi đơn hàng.

### 5.4 Add Item Logic

```
1. Resolve User by email
2. GetOrCreate Cart
3. Resolve Variant (check ACTIVE)
4. Check BANNED → throw PRODUCT_BANNED
5. Check DISCONTINUED → throw PRODUCT_DISCONTINUED
6. Check Available Stock (nếu stock != -1)
7. Check max_order_quantity (nếu != 0)
8. Merge với item hiện có (hoặc tạo mới)
9. Return CartResponse
```

### 5.5 Update Item Logic

- Nếu `quantity <= 0` → tự động remove item.
- Nếu `quantity > 0` → validate stock + max_order_quantity → update quantity.

---

## 6. Permission Mapping

| Permission | Role | Mô tả |
|---|---|---|
| `cart:view` | `USER` | Xem giỏ hàng của bản thân |
| `cart:manage` | `USER` | Thêm, sửa, xóa items trong giỏ |

> **Lưu ý:** Cả 2 permissions đều chỉ áp dụng cho giỏ hàng của **chính user đó** (owner-scoped). Không có admin permission cho cart.

---

## 7. Issues & Fixes

### Issue 1: Permission Naming Convention
**Vấn đề:** Initial seed dùng `CART_VIEW` (SCREAMING_SNAKE_CASE) không nhất quán với project.
**Fix:** Đổi toàn bộ sang `cart:view` và `cart:manage` (lowercase colon-separated) — đồng nhất với `product:read`, `product:create`, v.v.
**Files affected:** `V2.9` migration, `CartController.java`

### Issue 2: DB Constraint chk_stock_valid
**Vấn đề:** Constraint cũ `CHECK (stock_quantity >= 0)` không cho phép giá trị `-1` (vô hạn).
**Fix:** Thêm điều kiện `OR stock_quantity = -1` vào constraint trong `V2.9`.

### Issue 3: Unused Variable Lint Warning
**Vấn đề:** Biến `stock` trong `validateQuantityLimit()` được khai báo nhưng không dùng sau khi refactor.
**Fix:** Remove biến thừa, đọc trực tiếp từ entity.

---

## 8. Architecture Notes

- **Concurrency Safety:** Bảo vệ overselling bằng DB constraint + DB-level lock (không dùng in-memory lock để tránh race condition trên multi-instance).
- **Lazy Cart Creation:** Cart được tạo khi user gọi API lần đầu, không tạo khi register account.
- **Compile Verification:** `mvnw.cmd compile` — BUILD SUCCESS sau khi hoàn thành toàn bộ module.

---

## 9. Dependencies Cross-Module

| Module | Lý do phụ thuộc |
|---|---|
| `User` | Lookup user bằng email từ JWT |
| `Product / Variant` | Validate variant ACTIVE/BANNED/DISCONTINUED và kiểm tra tồn kho |
