# Rule: Permission Naming Convention

**Scope:** Toàn bộ project EasyMall Backend
**Applies to:** DB seed, `@PreAuthorize`, Spring Security, `messages.properties`

---

## 1. Format Chuẩn

```
{resource}:{action}
```

| Thành phần | Quy tắc | Ví dụ |
|---|---|---|
| `resource` | Lowercase, singular noun, dùng kebab-case nếu nhiều từ | `product`, `cart`, `order`, `product-image` |
| `action` | Lowercase, 1 trong các action chuẩn bên dưới | `read`, `create`, `update`, `delete`, `manage`, `view` |
| Separator | Dấu hai chấm `:` | `product:read` |

---

## 2. Action Vocabulary Chuẩn

| Action | Ý nghĩa | Dùng khi |
|---|---|---|
| `read` | Xem / lấy dữ liệu (public hoặc role cụ thể) | Resource công khai hoặc admin-only read |
| `view` | Xem dữ liệu của chính mình (owner-scoped) | Xem cart của user, xem đơn hàng của mình |
| `create` | Tạo mới resource | Admin tạo sản phẩm, category |
| `update` | Chỉnh sửa resource | Admin cập nhật sản phẩm |
| `delete` | Xóa (soft delete) resource | Admin xóa sản phẩm |
| `manage` | Bao gồm create + update + delete của chính mình | User quản lý giỏ hàng, đánh giá của mình |

---

## 3. Các Permissions Hiện Có Trong Project

### Product Module

| Permission | Role | Mô tả |
|---|---|---|
| `product:read` | `USER`, `ADMIN` | Xem danh sách và chi tiết sản phẩm |
| `product:create` | `ADMIN` | Tạo sản phẩm mới |
| `product:update` | `ADMIN` | Cập nhật sản phẩm |
| `product:delete` | `ADMIN` | Xóa sản phẩm (soft delete) |

### Category Module

| Permission | Role | Mô tả |
|---|---|---|
| `category:read` | `USER`, `ADMIN` | Xem danh sách và chi tiết category |
| `category:create` | `ADMIN` | Tạo category |
| `category:update` | `ADMIN` | Cập nhật category |
| `category:delete` | `ADMIN` | Xóa category |

### Cart Module

| Permission | Role | Mô tả |
|---|---|---|
| `cart:view` | `USER` | Xem giỏ hàng của chính mình |
| `cart:manage` | `USER` | Thêm, sửa, xóa items trong giỏ |

---

## 4. Cách Đặt Tên Theo Ownership

### Public / Admin Resource (CRUD)
Dùng `read`, `create`, `update`, `delete`:

```
product:read    → Ai cũng xem được
product:create  → Chỉ ADMIN
product:update  → Chỉ ADMIN
product:delete  → Chỉ ADMIN
```

### Owner-Scoped Resource (User tự quản lý)
Dùng `view` + `manage`:

```
cart:view    → User xem giỏ hàng CỦA MÌNH
cart:manage  → User thao tác giỏ hàng CỦA MÌNH
order:view   → User xem đơn hàng CỦA MÌNH
order:manage → User tạo, hủy đơn CỦA MÌNH
```

---

## 5. Anti-patterns ❌

| Sai | Đúng | Lý do |
|---|---|---|
| `CART_VIEW` | `cart:view` | SCREAMING_SNAKE_CASE không phải convention của project |
| `CART-VIEW` | `cart:view` | Không dùng dash cho separator |
| `cartView` | `cart:view` | camelCase không dùng cho permission |
| `view_cart` | `cart:view` | Thứ tự sai: action trước resource |
| `cart:VIEW` | `cart:view` | Action phải lowercase |
| `Cart:view` | `cart:view` | Resource phải lowercase |
| `carts:view` | `cart:view` | Resource dùng singular, không plural |

---

## 6. Vị Trí Áp Dụng

### 6.1 DB Migration (SQL)

```sql
INSERT INTO permissions (permission_name, description) VALUES
    ('cart:view',   'View own cart'),
    ('cart:manage', 'Manage own cart items')
ON CONFLICT (permission_name) DO NOTHING;
```

### 6.2 Controller (`@PreAuthorize`)

```java
@GetMapping("/me")
@PreAuthorize("hasAuthority('cart:view')")
public ApiResponse<CartResponse> getMyCart(Authentication authentication) { ... }

@PostMapping("/me/items")
@PreAuthorize("hasAuthority('cart:manage')")
public ApiResponse<CartResponse> addItem(...) { ... }
```

### 6.3 Không dùng `hasRole()` / `@Secured`

Project sử dụng **permission-based** (fine-grained), không dùng role-based authorization trực tiếp trong controller. Permission được seed vào DB và gán cho role qua bảng `role_permissions`.

```java
// ❌ KHÔNG dùng
@PreAuthorize("hasRole('ADMIN')")

// ✅ DÙNG
@PreAuthorize("hasAuthority('product:create')")
```

---

## 7. Checklist Khi Thêm Resource Mới

- [ ] Xác định resource name (singular, lowercase, kebab-case nếu cần)
- [ ] Xác định ownership: public admin CRUD hay owner-scoped?
  - Admin CRUD → dùng `read`, `create`, `update`, `delete`
  - Owner-scoped → dùng `view`, `manage`
- [ ] Tạo migration SQL seed permissions với đúng format
- [ ] Gán permissions vào đúng role trong migration
- [ ] Dùng `hasAuthority('resource:action')` trong `@PreAuthorize`
- [ ] Kiểm tra permission name khớp chính xác giữa DB seed và controller annotation

---

## 8. Template Migration Seed

```sql
-- Seed permissions cho {resource} module
INSERT INTO permissions (permission_name, description) VALUES
    ('{resource}:read',   'View {resource} list and details'),
    ('{resource}:create', 'Create new {resource}'),
    ('{resource}:update', 'Update existing {resource}'),
    ('{resource}:delete', 'Delete {resource}')
ON CONFLICT (permission_name) DO NOTHING;

-- Gán cho ADMIN
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM roles r CROSS JOIN permissions p
WHERE r.role_name = 'ADMIN'
  AND p.permission_name IN (
      '{resource}:read',
      '{resource}:create',
      '{resource}:update',
      '{resource}:delete'
  )
ON CONFLICT (role_id, permission_id) DO NOTHING;
```
