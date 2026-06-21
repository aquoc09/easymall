# BUSINESS LOGIC: CẬP NHẬT SẢN PHẨM

**Endpoint:** `PUT /api/v1/products/{id}`
**Payload:** JSON chứa thông tin sản phẩm cập nhật (Tương tự Create Product, các biến thể cũ cần truyền kèm `variant_id`).

### Giai đoạn 1: Pure Validation (Kiểm tra dữ liệu đầu vào)

- **Vận chuyển:** `weight_kg`, `length_m`, `width_m`, `height_m` \> 0.
- **Thông tin cơ bản:** Tên \< 150 ký tự, Slug \< 100 ký tự.
- **Phân loại:** `target_gender` thuộc \[0, 1, 2\] và `max_order_quantity` từ 1 đến 99.
- **Hình ảnh:** Bắt buộc 1 ảnh `is_thumbnail = true`.
- **Biến thể:** `options_config` \<= 2 nhóm, tối đa 50 phần tử.
- **Tổ hợp chập:** Tổng số biến thể gửi lên khớp độ dài cấu hình mảng.
- **Giá trị tồn kho và giá bán:** `stock_quantity` \>= -1, `price` \>= `cost_price` \> 0.

### Giai đoạn 2: Async Validation (Kiểm tra chéo Database)

- **Tính tồn tại:** Kiểm tra sản phẩm có ID truyền lên có tồn tại hay không.
- **Cấp danh mục:** Đảm bảo `category_id` mới là danh mục cấp cuối (Leaf Node).
- **Tính duy nhất:** Nếu `product_slug` thay đổi, kiểm tra trùng lặp với các sản phẩm khác trong DB. Các `sku_code` mới gửi lên cũng phải kiểm tra Unique.

### Giai đoạn 3: Database Transaction (Thực thi lưu trữ)

- Bắt buộc bọc toàn bộ khối lệnh bằng `BEGIN ... COMMIT`.
- **Cập nhật bảng products:** Ghi đè các trường cơ bản (Tên, Mô tả, Kích thước, Tags...). Trigger Full-text Search sẽ tự động cập nhật `search_vector`.
- **Cập nhật bảng product_images (Hard-Replace):** Chạy lệnh `DELETE` toàn bộ record có `product_id` tương ứng, sau đó `Bulk Insert` mảng images từ Payload vào.
- **Thuật toán Diff Orphan cho product_variants:** Khởi tạo một mảng rỗng `retainedVariantIds` để lưu các ID được ân xá.
- **Xử lý biến thể cũ (ID có sẵn trong Payload):**
  - Thêm ID này vào `retainedVariantIds`.
  - Truy vấn `old_stock` hiện tại. Nếu có chênh lệch (`new_stock` khác `old_stock`) và giá trị không phải -1, ghi 1 dòng `transaction_type = 'ADJUSTMENT'` vào bảng `inventory_transactions`.
  - Cập nhật giá bán, cấu hình (tuyệt đối không cập nhật `sku_code`).
- **Xử lý biến thể mới (ID = null trong Payload):**
  - Nếu thiếu `sku_code`, tự động Gen theo Rule đã định nghĩa.
  - Thực hiện `Insert` bản ghi mới.
  - Nếu `stock_quantity` \> 0, ghi log `IMPORT` vào `inventory_transactions`.
- **Dọn rác (Soft-delete):** Chạy lệnh `UPDATE product_variants SET is_active = false` đối với các bản ghi có `product_id` hiện tại nhưng không nằm trong mảng `retainedVariantIds`.

### Giai đoạn 4: Danh sách Exceptions (Bắt lỗi)

- Kế thừa toàn bộ mã lỗi 400 từ luồng Create (`InvalidCategoryLevelException`, `VariantPermutationMismatchException`...).
- `ProductNotFoundException` (404): Không tìm thấy sản phẩm cần cập nhật.
- `VariantNotFoundException` (400): Bắt lỗi bảo mật khi ID biến thể gửi lên không thuộc về sản phẩm hiện tại (tránh việc thao tác nhầm sang sản phẩm khác).
