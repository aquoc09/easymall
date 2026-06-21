# Session Analysis Report - Category Management Module

**Date:** 2026-06-21  
**Module:** Category (Danh mục sản phẩm đa cấp)  
**Status:** ✅ COMPLETED

---

## 1. Mục Tiêu Phiên Làm Việc

Thiết kế và triển khai đầy đủ API Backend quản lý danh mục sản phẩm (Category) hỗ trợ cây phân cấp đa cấp (Multi-level Tree) tuân thủ tiêu chuẩn kiến trúc hiện tại của dự án.

---

## 2. Quyết Định Kiến Trúc Quan Trọng

### 2.1 Không dùng Hibernate Lazy Loading Đệ Quy
Mặc dù có thể ánh xạ `@ManyToOne(parent)` và `@OneToMany(children)` trong `CategoryEntity`, nhưng quyết định **không thực hiện** vì:
- Gây ra nguy cơ N+1 Query khi Hibernate tự lazy-load từng nhánh con.
- Tổng số Query tăng tỉ lệ theo chiều sâu cây danh mục.

### 2.2 Thuật Toán O(N) Build Tree trên JVM
**Chiến lược thực hiện:**
1. Gọi duy nhất **1 câu SQL** lấy toàn bộ danh mục, đã sắp xếp theo `level ASC, display_order ASC`.
2. Duyệt vòng lặp O(N) qua `HashMap<Long, CategoryResponse>`:
   - Mỗi item được chuyển thành `CategoryResponse` và put vào Map.
   - Nếu `parentId == null` -> thêm vào list `rootCategories`.
   - Nếu có `parentId` -> lookup `parent` trong Map và `parent.getChildren().add(current)`.
3. Trả về `rootCategories` đã chứa đầy đủ cây lồng nhau.

**Kết quả:** Cấu trúc JSON Tree được build hoàn toàn trong bộ nhớ JVM với chi phí duy nhất 1 DB Round-trip.

### 2.3 Không Cho Phép Đổi `parentId` (No Re-parenting)
Để ngăn chặn lỗi **Circular Reference** (Ví dụ: A là cha của B, nhưng sau đó sửa để B làm cha của A), `CategoryUpdateRequest` cố tình **không khai báo field `parentId`**. Đây là cơ chế bảo vệ từ tầng API/DTO.

---

## 3. Các Files Đã Tạo / Chỉnh Sửa

| File | Trạng thái | Ghi chú |
|---|---|---|
| `util/SlugUtils.java` | ✅ Mới | Chuyển chuỗi Unicode tiếng Việt -> ASCII slug (vd: "Đồ Gia Dụng" → "do-gia-dung") |
| `entity/CategoryEntity.java` | ✅ Mới | JPA Entity ánh xạ bảng `categories` |
| `repository/CategoryRepository.java` | ✅ Mới | Spring Data JPA, 4 custom queries phục vụ business logic |
| `dtos/request/category/CategoryCreateRequest.java` | ✅ Mới | Validation messages dùng property key |
| `dtos/request/category/CategoryUpdateRequest.java` | ✅ Mới | Không có field `parentId` (by design) |
| `dtos/response/category/CategoryResponse.java` | ✅ Mới | Có field `List<CategoryResponse> children` để hỗ trợ JSON Tree |
| `mapper/CategoryMapper.java` | ✅ Mới | Builder Pattern thay thế MapStruct (do thiếu dependency) |
| `service/category/CategoryService.java` | ✅ Mới | Interface |
| `service/category/impl/CategoryServiceImpl.java` | ✅ Mới | 100% Business Logic |
| `controller/CategoryController.java` | ✅ Mới | 5 endpoints: Public Tree + Admin CRUD |
| `exception/ErrorCode.java` | ✅ Chỉnh sửa | Thêm 6 error code mới từ 6001–6006 |
| `resources/messages.properties` | ✅ Chỉnh sửa | Thêm error, success và validation message keys |

---

## 4. Business Logic chi tiết đã Implement

### 4.1 Phân tách Public vs Admin API
| Endpoint | Auth | Dữ liệu trả về |
|---|---|---|
| `GET /api/v1/categories/public` | Không cần | Chỉ danh mục có `status = 1` (Hiện) |
| `GET /api/v1/categories` | `category:read` | Tất cả danh mục (kể cả đã ẩn) |

### 4.2 Create Category Flow
1. `SlugUtils.toSlug(categoryName)` để tạo `category_code`.
2. Kiểm tra `category_code` trùng lặp -> ném `CATEGORY_CODE_ALREADY_EXISTS`.
3. Nếu `parentId != null`:
   - Tìm danh mục cha. Nếu không có -> ném `PARENT_CATEGORY_NOT_FOUND`.
   - Tính `level = parent.level + 1`. Nếu `level > 3` -> ném `MAX_LEVEL_EXCEEDED`.
4. Set các giá trị mặc định (`categoryStatus = 1`, `categoryType = STANDARD`).

### 4.3 Update Category Flow (Cascade Toggle)
1. Cập nhật các field cho phép (`categoryName`, `categoryStatus`, `iconUrl`, ...).
2. Nếu trạng thái thay đổi từ **Hiển thị (1) sang Ẩn (0)**:
   - Gọi `cascadeHideChildren(categoryId)` để đệ quy ẩn toàn bộ nhánh con.
   - Dùng `HashMap` để map các quan hệ cha-con.
   - Duyệt đệ quy qua `gatherDescendantsToHide()` và `saveAll()` một lần.

### 4.4 Delete Category (Hard Delete với Guards)
1. Kiểm tra `categoryId` tồn tại -> ném `CATEGORY_NOT_FOUND` nếu không.
2. `countByParentId(categoryId) > 0` -> ném `CATEGORY_HAS_CHILDREN`.
3. `checkCategoryHasProducts(categoryId)` -> Skeleton, sẽ tích hợp khi có module Product.

---

## 5. Error Codes đã Thêm Mới

| Code | Key | HTTP Status | Mô tả |
|---|---|---|---|
| 6001 | `CATEGORY_NOT_FOUND` | 404 | Danh mục không tồn tại |
| 6002 | `CATEGORY_CODE_ALREADY_EXISTS` | 409 | Slug/Code đã tồn tại |
| 6003 | `PARENT_CATEGORY_NOT_FOUND` | 400 | Danh mục cha không tồn tại |
| 6004 | `MAX_LEVEL_EXCEEDED` | 400 | Vượt quá 3 cấp danh mục |
| 6005 | `CATEGORY_HAS_CHILDREN` | 409 | Có danh mục con, không thể xoá |
| 6006 | `CATEGORY_IN_USE` | 409 | Đang có sản phẩm, không thể xoá |

---

## 6. Postman Collection

**Collection:** `EasyMall Category API` đã được tạo trong workspace `My Workspace` qua `mcp:postman-mcp-server`.

**Các request nổi bật:**
- `POST /api/v1/auth/login` (Admin) — Tự động lưu token vào biến `{{accessToken}}`.
- `POST /api/v1/categories` — Tạo Level 1, Level 2, Level 3 kèm test data mẫu.
- `[ERROR TEST] Create Level 4` — Kiểm tra lỗi `MAX_LEVEL_EXCEEDED`.
- `[CASCADE TEST] Hide Parent` — Kiểm tra ẩn dây chuyền.
- `[ERROR TEST] Delete Parent` — Kiểm tra lỗi `CATEGORY_HAS_CHILDREN`.

---

## 7. Điểm Cần Chú Ý & TODO Trong Tương Lai

- **`checkCategoryHasProducts()`**: Hiện tại trả về `false` cứng. Cần tích hợp khi module Product được implement.
- **Không có `MapStruct`**: `pom.xml` chưa khai báo dependency MapStruct. `CategoryMapper` hiện tại dùng Builder Pattern thủ công. Nếu muốn chuẩn hóa, cần thêm:
  ```xml
  <dependency>
      <groupId>org.mapstruct</groupId>
      <artifactId>mapstruct</artifactId>
      <version>1.5.5.Final</version>
  </dependency>
  ```
- **Category Permissions**: Cần seed thêm các permission `category:read`, `category:create`, `category:update`, `category:delete` vào database nếu muốn test admin endpoints.
