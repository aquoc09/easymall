# Báo cáo Cải tiến Kiến trúc: Order Snapshot & Product Hard Delete

**Ngày thực hiện:** 12/07/2026
**Tính năng:** Tái cấu trúc cơ chế xóa Sản phẩm (từ Soft Delete sang Hard Delete) kết hợp ứng dụng Snapshot Pattern cho Chi tiết Hóa Đơn.

---

## 1. Bối cảnh & Vấn đề

Trong phiên bản cũ, hệ thống áp dụng cơ chế **Soft Delete** (cờ `in_stock = false` và `is_active = false`) khi xóa một sản phẩm hoặc variant. Lý do chính là vì dữ liệu hóa đơn (`order_details`) đang liên kết khóa ngoại cứng (`variant_id`) trực tiếp vào `product_variants`. Nếu xóa cứng (Hard Delete) bảng `product_variants`, hệ thống sẽ ném lỗi vi phạm khóa ngoại (Foreign Key Constraint Violation) bảo vệ tính toàn vẹn của lịch sử hóa đơn.

**Nhược điểm của Soft Delete cũ:**
- Rác dữ liệu (Data bloat): Lâu dài, DB sẽ phình to với các dữ liệu rác, ảnh, biến thể... mà Admin đã thực sự muốn xóa.
- Khi truy vấn (Query) luôn phải thêm điều kiện `is_active = true`, làm giảm hiệu năng (overhead).
- Bị rườm rà ở logic cập nhật Product: Quá trình update phải kiểm tra "Variant này đã từng có người mua chưa?" để quyết định việc Soft Delete hay Hard Delete những variant bị Admin loại bỏ.

---

## 2. Giải pháp: Snapshot Pattern

Để giải quyết vấn đề rác dữ liệu mà vẫn giữ nguyên được lịch sử hóa đơn, chúng tôi áp dụng **Snapshot Pattern** (Chụp nhanh trạng thái dữ liệu). 

Khi người dùng thực hiện **Checkout** (Đặt hàng), hệ thống không chỉ lưu `variant_id` mà sẽ "chụp" toàn bộ thông tin quan trọng của sản phẩm tại thời điểm đó và lưu thẳng vào bảng `order_details`. 

Những trường được lưu snapshot bao gồm:
- Tên sản phẩm (`product_name`)
- Mã SKU (`sku_code`)
- Thông số biến thể (`variant_attributes`)
- Hình ảnh biến thể (`variant_image`)
- Giá tiền (`order_detail_price`)

Do thông tin hóa đơn đã mang đầy đủ thuộc tính để hiển thị, việc tồn tại `variant_id` gốc trở nên không còn quan trọng. Khóa ngoại có thể được ngắt một cách an toàn mà không làm mất thông tin hiển thị của Lịch sử Đơn Hàng.

---

## 3. Các thay đổi kỹ thuật (Implementation)

### 3.1. Schema Database (Flyway)
- **Tạo migration `V6.3__alter_order_details_add_snapshot_columns.sql`:**
  - Thêm 4 cột lưu trữ snapshot vào bảng `order_details`.
  - Thay đổi tính chất của cột `variant_id` cho phép `NULL`.
  - Đổi chế độ khóa ngoại từ `ON DELETE RESTRICT` thành `ON DELETE SET NULL`.

### 3.2. Mapping & Entity (JPA)
- **`OrderDetailEntity`:** Cấu hình `@ManyToOne` của `variant` sang dạng Nullable và gắn thêm `@OnDelete(action = OnDeleteAction.SET_NULL)` để đồng bộ với DB.
- **`OrderMapper`:** Thay vì trỏ qua `detail.getVariant().getProduct().getProductName()`, nay Mapper lấy trực tiếp giá trị từ các trường Snapshot. Điều này ngăn chặn triệt để lỗi `NullPointerException` khi `variant` đã bị xóa trong Database.
- Bổ sung `variantImage` trong `OrderDetailResponse` để Front-end có thể hiển thị ảnh trong trang chi tiết đơn hàng.

### 3.3. Xử lý Logic (Service)
- **`OrderServiceImpl`:** Bổ sung bước copy dữ liệu name, attributes, sku, và image từ Variant sang OrderDetail trong tiến trình xử lý giỏ hàng (`checkout`).
- **`CartItemRepository`:** Thêm Query xóa cứng các item trong giỏ hàng nếu Variant tương ứng bị xóa khỏi hệ thống.
- **`ProductServiceImpl`:**
  - **Khi Delete Product:** Ngắt khóa ngoại (set NULL cho các hóa đơn cũ), Xóa sản phẩm khỏi mọi giỏ hàng đang chờ, sau đó thực hiện lệnh Hard Delete hoàn toàn sản phẩm khỏi Database bằng `productRepository.delete()`.
  - **Khi Update Product:** Tối ưu hóa lại luồng check. Những variant bị loại bỏ khỏi danh sách update sẽ bị ngắt khóa ngoại và Hard Delete thẳng tay mà không cần phân nhánh logic Soft/Hard Delete như trước.

---

## 4. Kết quả và Đánh giá

- **Tối ưu Database:** Dung lượng lưu trữ tối ưu do dữ liệu đã bị xóa (deleted products/variants) hoàn toàn biến mất khỏi Database.
- **Tối ưu Codebase:** Xóa bỏ được các luồng logic phức tạp xung quanh cờ `isActive`.
- **An toàn Dữ liệu Hóa đơn:** Đảm bảo được tính bất biến (immutability) của hóa đơn. Giả sử sau này Admin đổi tên sản phẩm hay đổi hình ảnh, các hóa đơn cũ vẫn giữ nguyên thông tin được snapshot ở thời điểm mua, phản ánh chính xác 100% tình trạng mua bán trong quá khứ.
- **Code Clean:** Mã nguồn không có cảnh báo nào và biên dịch thành công 100% (`BUILD SUCCESS`). Cấu trúc Clean Code được duy trì.

---
*Báo cáo kết thúc.*
