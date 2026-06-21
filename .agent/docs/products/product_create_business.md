# BUSINESS LOGIC: SELLER TẠO / CẬP NHẬT SẢN PHẨM

## 1. Endpoints

- **POST** `/api/v1/products`
- **PUT** `/api/v1/products/{id}`

---

## 2. Các Giai đoạn Thực thi (Flow)

### Validate Danh mục (Leaf Node)

Danh mục được chọn BẮT BUỘC phải là danh mục cấp cuối cùng (Không có danh mục con).

### Giai đoạn 1: Pure Validation (Kiểm tra dữ liệu đầu vào)

1. **Kiểm tra Vận chuyển:** `weight_kg`, `length_m`, `width_m`, `height_m` phải > 0.
2. **Kiểm tra Thông tin cơ bản:**
   - Tên sản phẩm không quá 150 ký tự.
   - Slug không quá 100 ký tự.
   - `target_gender` nằm trong [0, 1, 2].
3. **Giới hạn số lượng:** 1 <= `max_order_quantity` <= 99.
4. **Kiểm tra Hình ảnh:** Bắt buộc có đúng 1 ảnh mang cờ `is_thumbnail = true`.
5. **Kiểm tra Biến thể & Chống Spam:**
   - `options_config` chứa tối đa 2 nhóm (VD: Màu sắc, Kích cỡ).
   - Mảng `variants` không vượt quá 50 phần tử.
   - Tổng số biến thể phải khớp với phép tính Tổ hợp chập (Cartesian Product) từ `options_config`.
   - `stock_quantity` >= -1 (Tồn kho vô cực).
   - Giá bán: `price` >= `cost_price` > 0.

### Giai đoạn 2: Async Validation (Kiểm tra chéo Database)

1. Truy vấn bảng categories: Đảm bảo `category_id` là danh mục cấp cuối (không có `parent_id` trỏ tới nó).
2. Kiểm tra tính duy nhất: Kiểm tra `product_slug` và các `sku_code` (nếu Seller tự nhập) đã tồn tại trong DB chưa.

### Giai đoạn 3: Thực thi Lưu trữ (Database Insert)

1. **Insert Product Base:** Lưu dữ liệu vào bảng `products`. Lấy `product_id` trả về.
2. **Insert Images:** Gắn `product_id` vào mảng hình ảnh và Bulk Insert vào `product_images`.
3. **Xử lý Variants:**
   - Nếu thiếu `sku_code`, hệ thống tự sinh dựa trên Rule: `[Prefix Danh Mục]-[ProductID]-[Thuộc Tính]`.
   - Format In hoa, bỏ ký tự O, 0, I, l.
   - Gắn `product_id` vào từng biến thể và Bulk Insert vào `product_variants`.
4. **Đồng bộ Tồn kho ban đầu:**
   - Chỉ tạo log vào bảng `inventory_transactions` cho biến thể có `stock_quantity` > 0 (Type: 'IMPORT').

---

## 3. Danh sách Exceptions (Bắt lỗi)

### Nhóm 400 - Lỗi Ràng buộc Nghiệp vụ

- `InvalidCategoryLevelException`: "Vui lòng chọn danh mục chi tiết nhất (Cấp cuối cùng)."
- `ShippingDimensionInvalidException`: "Kích thước và trọng lượng đóng gói phải lớn hơn 0."
- `VariantOptionLimitExceededException`: "Chỉ hỗ trợ tối đa 2 nhóm phân loại và 50 biến thể để chống spam."
- `VariantPermutationMismatchException`: "Cấu hình phân loại không khớp với danh sách biến thể gửi lên."
- `MissingThumbnailImageException`: "Vui lòng chọn 1 hình ảnh làm Ảnh đại diện (Thumbnail)."

### Ràng buộc chuẩn hóa mã SKU

- `ERR_SKU_LENGTH`: Độ dài 10 - 20 ký tự.
- `ERR_SKU_FORMAT`: Sai format `[DANH MỤC]-[ID]-[MÀU]-[SIZE]`.
- `ERR_SKU_INVALID_CHARS`: Chứa ký tự cấm (O, 0, I, l).
- `ERR_SKU_LOWERCASE`: Chứa chữ thường.

### Chống Spam & Business Logic khác

- `ERR_NAME_TOO_LONG`: Tên sản phẩm > 150 ký tự hoặc viết HOA toàn bộ.
- `ERR_SLUG_INVALID`: Sai format (chỉ cho phép a-z, 0-9, dấu -).
- `ERR_SLUG_DUPLICATE`: Slug đã tồn tại.
- `ERR_PRICE_INVALID`: `price` hoặc `cost_price` <= 0 hoặc `price` < `cost_price`.
- `ERR_STOCK_NEGATIVE`: `stock_quantity` hoặc `locked_stock` < 0.
- `ERR_VARIANTS_EMPTY`: Sản phẩm phải có ít nhất 1 variant.
- `ERR_IMAGES_LIMIT`: Vượt quá số lượng ảnh cho phép.

---

## 4. Database Transaction Flow

**BẮT BUỘC** bọc các bước trong SQL Transaction (`BEGIN ... COMMIT`) để đảm bảo tính toàn vẹn dữ liệu.

1. Validate Payload.
2. Insert Product Base.
3. Insert Product Images.
4. Xử lý & Insert Variants.
5. Khởi tạo Tồn kho (Inventory Sync).

---

## 5. Dùng Redis TTL (Key Expiration)

Sử dụng cho các trường hợp như giữ chỗ tồn kho (Locked Stock).

- **Cơ chế:** Lưu key vào Redis với TTL 15 phút. Sử dụng _Keyspace Notifications_ để bắt sự kiện hết hạn.
- **Spring Boot:** Cấu hình `MessageListener` để xử lý sự kiện, kiểm tra trạng thái thanh toán và giải phóng `locked_stock` nếu cần.
- **Lưu ý:** Cơ chế "Fire and Forget" của Pub/Sub có thể gây mất sự kiện nếu hệ thống khởi động lại.

---

## 6. Example JSON Structure

```json
{
  "product_id": 1,
  "product_slug": "airflow-supima-cotton-crew-neck-t-shirt",
  "product_name": "AirFlow Supima Cotton Crew Neck T-Shirt",
  "category_id": 1,
  "product_description": "Engineered from 100% premium long-staple Supima cotton.",
  "product_tags": ["Basic", "Supima", "Cotton", "Tee", "Everyday"],
  "in_popular": true,
  "in_stock": true,
  "target_gender": 2,
  "max_order_quantity": 5,
  "options_config": {
    "sizes": ["XS", "S", "M", "L", "XL", "XXL"],
    "colors": [
      { "colorName": "Off-White", "colorCode": "#FAF9F6" },
      { "colorName": "Ink Black", "colorCode": "#1C1C1C" },
      { "colorName": "Sage Olive", "colorCode": "#556B2F" }
    ]
  },
  "weight_kg": 0.3,
  "length_m": 0.2,
  "width_m": 0.15,
  "height_m": 0.03,
  "images": [
    {
      "image_id": 11,
      "image_url": "...",
      "is_thumbnail": true,
      "display_order": 1
    }
  ],
  "variants": [
    {
      "variant_id": 101,
      "sku_code": "TEE-WHT-XS",
      "price": 690000,
      "cost_price": 250000,
      "stock_quantity": 15,
      "variant_attributes": { "size": "XS", "colorName": "Off-White" }
    }
  ]
}
```
