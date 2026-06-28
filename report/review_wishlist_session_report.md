# Session Report: Review & Wishlist Module Implementation

**Date**: 2026-06-28
**Module**: EasyMall Core (Spring Boot) — Review & Wishlist Feature

---

## 1. Tổng Quan

Phiên làm việc này tập trung vào việc phân tích, thiết kế và triển khai hoàn chỉnh hai module mới: **Review** (đánh giá sản phẩm) và **Wishlist** (danh sách yêu thích), theo đúng kiến trúc layered đang có trong project.

---

## 2. Phân Tích & Thiết Kế

### 2.1 Phân Tích DB Schema
Dựa vào hai migration đã có:
- `V3.8__create_reviews_and_review_images_table.sql`
- `V4.0__create_wishlists_table.sql`

Các điểm thiết kế quan trọng được quyết định qua review với user:

| Vấn đề | Quyết định |
|--------|-----------|
| `order_id` FK bị CASCADE DELETE | Đổi thành **SET NULL** — review giữ lại dạng **anonymous** khi order bị xóa |
| Rating aggregation | **Tính toán động** qua aggregate query, không cache vào `products` table |
| Bảng `wishlists` thiếu UNIQUE constraint | Thêm constraint `(user_id, product_id)` qua migration mới |

### 2.2 Business Rules Xác Nhận
- User chỉ được review sản phẩm khi đơn hàng ở trạng thái **COMPLETED**
- Mỗi user chỉ được review 1 lần cho mỗi cặp `(order_id, product_id)`
- Review mặc định tạo với `status = PENDING`, chỉ `APPROVED` mới hiển thị public
- Wishlist dùng cơ chế **toggle**: POST cùng endpoint để add/remove

---

## 3. Các File Đã Tạo / Sửa

### 3.1 Migration

| File | Nội dung |
|------|----------|
| `V4.5__reseed_user_kiethuynh_and_address.sql` | Reseed user `kiethuynh3499@gmail.com` với password bcrypt + address mặc định |
| `V4.6__normalize_variant_attributes_keys.sql` | Chuẩn hóa JSONB key: `mau_sac` → `color`, `kich_co` → `size` trên toàn bộ `product_variants` |
| `V4.7__setup_review_wishlist.sql` | Alter reviews FK (CASCADE → SET NULL), UNIQUE constraint wishlist, seed 6 permissions mới |

### 3.2 Enum

| File | Nội dung |
|------|----------|
| `enums/ReviewStatus.java` | `PENDING`, `APPROVED`, `HIDDEN` |

### 3.3 Entity

| File | Nội dung |
|------|----------|
| `entity/ReviewEntity.java` | Map bảng `reviews`, `order` field nullable (anonymous) |
| `entity/ReviewImageEntity.java` | Map bảng `review_images` |
| `entity/WishlistEntity.java` | Map bảng `wishlists`, khai báo `@UniqueConstraint` |

### 3.4 Repository

| File | Nội dung |
|------|----------|
| `repository/ReviewRepository.java` | Query aggregate động: `AVG(rating)`, `COUNT` by rating, tìm theo user/product/status |
| `repository/WishlistRepository.java` | `findByUser_UserIdAndProduct_ProductId`, `deleteByUser_UserIdAndProduct_ProductId` |

### 3.5 DTO

| File | Nội dung |
|------|----------|
| `dtos/request/review/CreateReviewRequest.java` | Validation với message keys từ `messages.properties` |
| `dtos/request/review/UpdateReviewStatusRequest.java` | Validation với message key `{validation.reviewStatus.not-null}` |
| `dtos/response/review/ReviewResponse.java` | `userFullName = "Ẩn danh"` khi anonymous |
| `dtos/response/review/ReviewImageResponse.java` | `reviewImageId`, `imageUrl` |
| `dtos/response/review/ReviewSummaryResponse.java` | `averageRating`, `totalReviews`, `ratingBreakdown` (Map 1–5) |
| `dtos/response/review/WishlistResponse.java` | `minPrice` từ variants, `thumbnailUrl` từ images đầu tiên |

### 3.6 Service

| File | Nội dung |
|------|----------|
| `service/review/ReviewService.java` | Interface |
| `service/review/impl/ReviewServiceImpl.java` | Validate COMPLETED order, check duplicate, build ratingBreakdown |
| `service/review/WishlistService.java` | Interface với `toggleWishlist()` trả về `boolean` |
| `service/review/impl/WishlistServiceImpl.java` | Toggle logic, map minPrice/thumbnail từ lazy-loaded collections |

### 3.7 Controller

| File | Nội dung |
|------|----------|
| `controller/ReviewController.java` | 5 endpoints, dùng `@permissionChecker`, public routes không cần auth |
| `controller/WishlistController.java` | 3 endpoints, toggle response `Map<String, Boolean>` |

### 3.8 Exception & Messages

| File | Thay đổi |
|------|---------|
| `exception/ErrorCode.java` | Thêm 5 error codes: `REVIEW_NOT_FOUND`, `REVIEW_ALREADY_EXISTS`, `REVIEW_ORDER_NOT_COMPLETED`, `REVIEW_ORDER_OWNERSHIP_DENIED`, `WISHLIST_ITEM_NOT_FOUND` |
| `resources/messages.properties` | Thêm 3 nhóm: error messages, success messages, validation messages cho Review & Wishlist |

---

## 4. API Endpoints Đã Triển Khai

### Review — `/api/v1/reviews`

| Method | Path | Permission | Mô tả |
|--------|------|-----------|-------|
| `POST` | `/` | `review:create` | Tạo review |
| `GET` | `/product/{productId}` | Public | Reviews APPROVED của sản phẩm |
| `GET` | `/product/{productId}/summary` | Public | Rating summary (avg + breakdown) |
| `GET` | `/me` | `review:view` | Reviews của chính user |
| `PATCH` | `/{id}/status` | `review:moderate` | Admin duyệt/ẩn |
| `DELETE` | `/{id}` | `review:delete` | Xóa review |

### Wishlist — `/api/v1/wishlists`

| Method | Path | Permission | Mô tả |
|--------|------|-----------|-------|
| `POST` | `/{productId}` | `wishlist:manage` | Toggle add/remove |
| `GET` | `/me` | `wishlist:view` | Danh sách wishlist |
| `DELETE` | `/{productId}` | `wishlist:manage` | Xóa explicit |

---

## 5. Postman Collection

Đã tạo **10 requests** trong collection **EasyMall** (Postman):
- Folder `Reviews`: 7 requests (bao gồm 1 error test)
- Folder `Wishlists`: 4 requests (bao gồm 1 error test)

Tất cả requests dùng `{{baseUrl}}`, `{{userToken}}`, `{{adminToken}}` variables. Test user: `kiethuynh3499@gmail.com` / `Password@123`.

---

## 6. Vấn Đề Đã Fix Trong Phiên

| Vấn đề | Fix |
|--------|-----|
| `variant_attributes` trả về key `mau_sac`/`kich_co` | Migration V4.6 đổi toàn bộ sang `color`/`size` bằng JSONB operator |
| Validation messages hardcoded trong DTO | Chuyển sang message keys `{validation.x.y}` tập trung trong `messages.properties` |
| Unused import `AccessDeniedException` | Đã xóa khỏi `ReviewServiceImpl.java` |

---

## 7. Kết Quả Compile

```
[INFO] BUILD SUCCESS
```
Toàn bộ code compile sạch không có lỗi. Chỉ có 2 warning cũ về deprecated `RestTemplateBuilder` không liên quan.

---

## 8. Next Steps

- Restart Spring Boot để Flyway chạy migration V4.5, V4.6, V4.7
- Test luồng: Login `kiethuynh3499@gmail.com` → Checkout order → Admin chuyển status COMPLETED → Tạo Review
- Cân nhắc bổ sung field `reply_content` vào `reviews` table nếu cần tính năng Admin reply review
- Upload ảnh review qua Cloudinary là bước riêng (chưa implement), hiện tại client truyền URL trực tiếp
