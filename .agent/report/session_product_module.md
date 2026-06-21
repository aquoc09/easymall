# Session Report — Product, Variants, Images & Inventory Module

**Date:** 2026-06-21
**Modules:** Product · ProductVariant · ProductImage · InventoryTransaction
**Status:** ✅ COMPLETED — `mvn clean compile` BUILD SUCCESS

---

## 1. Executive Summary

Phiên làm việc này implement toàn bộ module Product bao gồm Product, Product Variants, Product Images và InventoryTransaction. Hệ thống đã được tích hợp đầy đủ với MapStruct cho object mapping, SKU Generation theo cấu trúc chuẩn, và bảo mật phân cấp với `product:read` (public) và các permission admin.

**Key Achievements:**
- Implement đầy đủ CRUD cho Product, bao gồm nested Variants và Images.
- Tích hợp **MapStruct** với `@BeanMapping` + `@MappingTarget` cho partial update (PATCH semantics).
- Tự động sinh **SKU** theo cấu trúc `{CATEGORY_CODE}-{PRODUCT_ID}-{ATTRIBUTES}-{RANDOM}`.
- Triển khai **Soft Delete** cho Product (set `inStock = false`, deactivate variants).
- Hoàn thiện `checkCategoryHasProducts()` trong Category module (tích hợp cross-module).
- **Postman Collection** đã được tạo trong workspace Postman cá nhân.

---

## 2. Database Migrations

| File | Nội dung |
|---|---|
| `V2.2__create_product_table.sql` | Bảng `products` với đầy đủ columns (JSONB: `product_tags`, `options_config`; full-text search: `search_vector`) |
| `V2.3__create_product_variants_and_images_table.sql` | Bảng `product_variants` (JSONB: `variant_attributes`) và `product_images` |
| `V2.4__create_inventory_transactions_table.sql` | Bảng `inventory_transactions` (do user tạo) |
| `V2.5__seed_product_permissions.sql` | Seed 4 permissions: `product:read`, `product:create`, `product:update`, `product:delete` |

> **Note:** V2.4 được user tạo trực tiếp trên database — AI chỉ tham chiếu, không tạo file này.

---

## 3. Các Files Đã Tạo / Chỉnh Sửa

### 3.1 New Files

| Layer | File | Mô tả |
|---|---|---|
| **Enum** | `enums/InventoryTransactionType.java` | `IN`, `OUT`, `ADJUSTMENT`, `RESERVE`, `RELEASE` |
| **Entity** | `entity/ProductEntity.java` | JPA entity cho bảng `products`. JSONB fields lưu dạng `String`. |
| **Entity** | `entity/ProductVariantEntity.java` | Variant entity với `@ManyToOne(Product)` và `@OneToMany(InventoryTransaction)` |
| **Entity** | `entity/ProductImageEntity.java` | Image entity với `@ManyToOne(Product)` |
| **Entity** | `entity/InventoryTransactionEntity.java` | Transaction entity — ghi nhận lịch sử tồn kho |
| **Utility** | `util/SkuGenerator.java` | Sinh SKU theo format chuẩn |
| **Repository** | `repository/ProductRepository.java` | JPA repository + custom queries (`findBySlug`, `existsBySlug`, `existsByCategoryId`) |
| **Repository** | `repository/ProductVariantRepository.java` | `existsBySkuCode()` |
| **Repository** | `repository/ProductImageRepository.java` | JPA repository |
| **Repository** | `repository/InventoryTransactionRepository.java` | JPA repository |
| **DTO Request** | `dtos/request/product/ProductCreateRequest.java` | Create product payload |
| **DTO Request** | `dtos/request/product/ProductUpdateRequest.java` | Partial update payload (all fields nullable) |
| **DTO Request** | `dtos/request/product/ProductVariantRequest.java` | Variant payload (nested trong product request) |
| **DTO Request** | `dtos/request/product/ProductImageRequest.java` | Image payload (nested trong product request) |
| **DTO Response** | `dtos/response/product/ProductResponse.java` | Full product response (incl. nested variants, images) |
| **DTO Response** | `dtos/response/product/ProductVariantResponse.java` | Variant response |
| **DTO Response** | `dtos/response/product/ProductImageResponse.java` | Image response |
| **Mapper** | `mapper/ProductMapper.java` | **MapStruct abstract class** — full mapping + JSONB handling |
| **Service** | `service/product/ProductService.java` | Interface |
| **Service** | `service/product/impl/ProductServiceImpl.java` | Business logic implementation |
| **Controller** | `controller/ProductController.java` | REST API endpoints |

### 3.2 Modified Files

| File | Thay đổi |
|---|---|
| `pom.xml` | Thêm `mapstruct` dependency + `mapstruct-processor` trong `annotationProcessorPaths` |
| `exception/ErrorCode.java` | Thêm 7 Product error codes (7001–7007) |
| `resources/messages.properties` | Thêm error/success/validation messages cho product |
| `config/SecurityConfig.java` | Thêm public endpoints `/api/v1/products/public/**` |
| `service/category/impl/CategoryServiceImpl.java` | Implement `checkCategoryHasProducts()` thực sự (dùng `ProductRepository.existsByCategoryId()`) |
| `repository/CategoryRepository.java` | Thêm `existsByCategoryId()` method |

---

## 4. Architecture & Design Decisions

### 4.1 MapStruct Integration

**Vấn đề:** Trước đây, Category mapper dùng Builder Pattern thủ công vì chưa có MapStruct dependency.

**Giải pháp:**
- Thêm MapStruct vào `pom.xml`, đặt processor **sau** Lombok trong `annotationProcessorPaths`.
- `ProductMapper` là **abstract class** (không phải interface) để có thể inject `ObjectMapper` qua `@Autowired` field và viết helper methods.
- JSONB fields (`productTags`, `optionsConfig`, `variantAttributes`) được xử lý trong `@AfterMapping` để tránh MapStruct không biết cách convert `String ↔ List/Map`.

```java
@Mapper(
    componentModel = "spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public abstract class ProductMapper {
    @Autowired
    protected ObjectMapper objectMapper;

    // ... abstract methods + @AfterMapping helpers
}
```

### 4.2 Partial Update (PATCH Semantics)

**Vấn đề ban đầu:** `updateProduct()` có ~35 dòng `if (field != null) { entity.setField(field); }` — verbose và error-prone.

**Giải pháp:** `updateEntityFromRequest()` với `@BeanMapping` + `@MappingTarget`:

```java
@BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
@Mapping(target = "productId", ignore = true)
// ... các ignore khác
public abstract void updateEntityFromRequest(ProductUpdateRequest request,
                                            @MappingTarget ProductEntity entity);
```

**Kết quả:** `updateProduct()` giảm từ ~35 dòng xuống còn 1 dòng gọi mapper:
```java
productMapper.updateEntityFromRequest(request, product);
```

### 4.3 SKU Generation

Format: `{CAT_CODE}-{PRODUCT_ID}-{ATTR_HASH}-{RANDOM_4}`

| Segment | Ví dụ | Mô tả |
|---|---|---|
| `CAT_CODE` | `AOT` | 3 ký tự đầu của `category_code` (bỏ dấu `-`), viết hoa |
| `PRODUCT_ID` | `00042` | productId pad 5 chữ số |
| `ATTR_HASH` | `RE-XL` | Ghép attributes (vd: `{"color":"Red","size":"XL"}` → `RE-XL`) |
| `RANDOM_4` | `K3M9` | 4 ký tự alphanumeric random để đảm bảo uniqueness |

Ví dụ đầy đủ: `AOT-00042-RE-XL-K3M9`

### 4.4 Security Model

| Endpoint | Method | Permission | Ghi chú |
|---|---|---|---|
| `/api/v1/products/public` | GET | Public | Tất cả sản phẩm — storefront |
| `/api/v1/products/public/{id}` | GET | Public | Chi tiết theo ID |
| `/api/v1/products/public/slug/{slug}` | GET | Public | Chi tiết theo slug — SEO friendly |
| `/api/v1/products` | POST | `product:create` | Admin tạo mới |
| `/api/v1/products/{id}` | PUT | `product:update` | Admin cập nhật |
| `/api/v1/products/{id}` | DELETE | `product:delete` | Admin soft delete |

### 4.5 JSONB Fields

Entity lưu JSONB fields dưới dạng `String` (PostgreSQL JSONB type), Mapper convert tự động:

| Field | Java Type (DTO) | DB Type | Mapper Strategy |
|---|---|---|---|
| `productTags` | `List<String>` | JSONB | `@AfterMapping` — serialize/deserialize |
| `optionsConfig` | `String` (raw JSON) | JSONB | Validate format + pass through |
| `variantAttributes` | `Map<String, String>` | JSONB | `@AfterMapping` — serialize/deserialize |

### 4.6 Soft Delete

`deleteProduct()` không xóa bản ghi khỏi DB:
1. Set `product.inStock = false` → ẩn khỏi storefront.
2. Set `product.inPopular = false`.
3. Deactivate tất cả variants: `variant.isActive = false`.

---

## 5. Error Codes Mới (7xxx)

| Code | Key | HTTP | Mô tả |
|---|---|---|---|
| 7001 | `PRODUCT_NOT_FOUND` | 404 | Sản phẩm không tồn tại |
| 7002 | `PRODUCT_SLUG_ALREADY_EXISTS` | 409 | Slug đã tồn tại |
| 7003 | `SKU_ALREADY_EXISTS` | 409 | SKU code trùng lặp |
| 7004 | `INVALID_JSONB_FORMAT` | 400 | JSON không hợp lệ cho JSONB field |
| 7005 | `CATEGORY_NOT_FOUND_FOR_PRODUCT` | 400 | Category không tồn tại khi tạo product |
| 7006 | `PRODUCT_VARIANT_NOT_FOUND` | 404 | Variant không tồn tại |
| 7007 | `PRODUCT_HAS_ACTIVE_ORDERS` | 409 | Sản phẩm đang có đơn hàng |

---

## 6. Business Logic chi tiết

### 6.1 Create Product Flow
```
1. Validate categoryId (nếu có) → existsById()
2. productMapper.toEntity(request) — MapStruct map simple fields
3. @AfterMapping xử lý JSONB: productTags, optionsConfig
4. ensureUniqueSlug(baseSlug) — append "-2", "-3",... nếu trùng
5. productRepository.save(product) — lấy productId
6. buildAndSaveVariants() — sinh SKU, save từng variant
7. buildAndSaveImages() — save từng image
8. findById() reload — load full entity với associations
9. productMapper.toResponse(product) → return
```

### 6.2 Update Product Flow (Partial)
```
1. findById(productId) or throw PRODUCT_NOT_FOUND
2. Validate categoryId nếu thay đổi, set vào entity
3. productMapper.updateEntityFromRequest(request, product)
   → MapStruct tự skip null fields (NullValuePropertyMappingStrategy.IGNORE)
   → @AfterMapping xử lý JSONB fields nếu có
4. Variants: nếu request.variants != null → clear + re-add (orphan removal)
5. Images: nếu request.images != null → clear + re-add
6. save + reload + toResponse()
```

### 6.3 Delete Product Flow
```
1. findById(productId) or throw PRODUCT_NOT_FOUND
2. product.setInStock(false)
3. product.setInPopular(false)
4. product.variants.forEach(v -> v.setIsActive(false))
5. productRepository.save(product)
```

---

## 7. Cross-Module Integration

### 7.1 Category ↔ Product
Sau khi implement module Product, `checkCategoryHasProducts()` trong `CategoryServiceImpl` đã được hoàn thiện:

```java
// CategoryServiceImpl.java
@Override
public boolean checkCategoryHasProducts(Long categoryId) {
    return productRepository.existsByCategoryId(categoryId);
}
```

Trước đó (trong session Category), method này return `false` cứng. Giờ đã integrate thực sự.

### 7.2 SKU Generation từ Category Code
`resolveCategoryCode()` trong `ProductServiceImpl` lookup `CategoryEntity` để lấy `categoryCode`, extract 3 ký tự đầu (bỏ dấu `-`) làm prefix SKU. Fallback về `"PRD"` nếu không có category.

---

## 8. MapStruct Build Notes

### pom.xml Configuration
```xml
<dependency>
    <groupId>org.mapstruct</groupId>
    <artifactId>mapstruct</artifactId>
    <version>1.5.5.Final</version>
</dependency>

<!-- Trong maven-compiler-plugin / annotationProcessorPaths -->
<path>
    <groupId>org.mapstruct</groupId>
    <artifactId>mapstruct-processor</artifactId>
    <version>1.5.5.Final</version>
</path>
```

> **Quan trọng:** MapStruct processor phải đặt **sau** Lombok trong `annotationProcessorPaths`.

### Generated Mapper
`ProductMapperImpl.java` được generate tại:
```
target/generated-sources/annotations/com/quocnva/easymall/mapper/ProductMapperImpl.java
```

Verified methods generated:
- `toEntity(ProductCreateRequest)`
- `updateEntityFromRequest(ProductUpdateRequest, ProductEntity)` ← mới
- `toResponse(ProductEntity)`
- `toVariantEntity(ProductVariantRequest)`
- `toVariantResponse(ProductVariantEntity)`
- `toImageEntity(ProductImageRequest)`
- `toImageResponse(ProductImageEntity)`

---

## 9. Postman Collection

**Collection:** `EasyMall Product API` đã được tạo trong workspace `My Workspace` qua MCP Postman tool.

**Cấu trúc collection:**

| Folder | Requests |
|---|---|
| **Auth** | POST Login (Admin) — lưu `{{accessToken}}` |
| **Public — Products** | GET All Products, GET by ID, GET by Slug |
| **Admin — Products** | POST Create Product (full + minimal), PUT Update Product, DELETE Product |
| **Variant & Image Tests** | POST Create với variants đa thuộc tính, Update variants |
| **Error Cases** | POST với invalid categoryId, POST SKU duplicate, POST invalid JSON |

**Variables:**
- `{{baseUrl}}` = `http://localhost:8080`
- `{{accessToken}}` — tự động set sau khi login

---

## 10. Build Verification

```
✅ mvn clean compile — BUILD SUCCESS
   [WARNING] Unmapped target property: "inStock" → Fixed with @Mapping(target="inStock", ignore=true)
```

Sau fix warning: `mvn clean compile` hoàn toàn clean, không có ERROR, không có WARNING.

---

## 11. Technical Debt & Future Improvements

| Item | Priority | Ghi chú |
|---|---|---|
| **InventoryTransaction Integration** | High | Entity và repo đã có, nhưng chưa có service logic ghi transaction khi tạo/update variant stock |
| **Search/Filter** | High | `search_vector` đã có trong DB (tsvector), cần implement full-text search endpoint |
| **Pagination** | Medium | `getAllProducts()` hiện return all — cần `Pageable` |
| **Image Upload** | Medium | Hiện chỉ lưu URL — cần integrate file upload service (S3, Cloudinary...) |
| **Product Status** | Low | Cân nhắc thêm `productStatus` field thay vì chỉ dùng `inStock` |
| **Cache** | Low | Cache product detail để giảm DB load |
| **Soft Delete Flag** | Low | `inStock = false` là soft delete logic — cần làm rõ hơn với `isDeleted` flag riêng |

---

## 12. Summary of Changes per Request

| Request | Action |
|---|---|
| Initial implementation | 13 steps: Entity, Repo, DTO, Mapper, Service, Controller, Security |
| MapStruct refactor | Thêm `updateEntityFromRequest()` với `@BeanMapping` + `@MappingTarget`, refactor `updateProduct()` từ 35 dòng → 1 dòng |
| Fix MapStruct warning | Thêm `@Mapping(target="inStock", ignore=true)` vào `toEntity()` |
| Postman Collection | Tạo collection `EasyMall Product API` trong Postman workspace |
