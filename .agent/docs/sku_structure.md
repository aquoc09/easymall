# Cấu Trúc SKU

### 1. Công Thức Chuẩn Đặt Mã SKU Thời Trang

Một mã SKU tiêu chuẩn thường dài từ **10 đến 16 ký tự**, phân cách nhau bằng dấu gạch ngang (-), được ghép từ các khối thông tin theo thứ tự từ lớn đến bé:
**[DANH MỤC] - [PRODUCT_ID] - [MÀU SẮC] - [KÍCH CỠ]**

#### Phân tích chi tiết từng khối:

1. **Danh mục (2-3 ký tự):**
   - AT (Áo thun), AP (Áo Polo), AK (Áo khoác), QD (Quần đùi), SH (Shoes/Giày).
2. **Màu sắc (2-3 ký tự):**
   - BLK (Black - Đen), WHT (White - Trắng), NVY (Navy - Xanh đen), RED (Đỏ).
3. **Kích cỡ (1-2 ký tự):**
   - S, M, L, XL (Cho quần áo).
   - 40, 41, 42 (Cho giày dép).

### 2. Ví dụ Thực Tế Cho Từng Sản Phẩm

- **Trường hợp 1: Áo Polo Fred Perry Vintage, Màu Xanh Navy, Size M**
  - **Mã SKU:** AP-1-NVY-M
  - _Nhân viên kho nhặt hàng nhìn vào sẽ nhẩm đọc: "Áo Polo - Fred Perry - Mẫu Vintage 01 - Navy - Size M"._
- **Trường hợp 2: Giày chạy bộ Mizuno Wave Rider, Màu Trắng, Size 42**
  - **Mã SKU:** SH-2-WHT-42
- **Trường hợp 3: Quần Tây Nam YSL ống suông, Đen, Size 32**
  - **Mã SKU:** QT-3-BLK-32

### 3. Quy Tắc "Sống Còn" Khi Thiết Kế SKU Cho Hệ Thống

Khi lưu trữ mã này vào cột `sku_code` trong bảng `product_variants`, cần thiết lập các quy tắc sau:

- **Không dùng các ký tự dễ gây lú lẫn:** Tránh sử dụng chữ O và số 0, hoặc chữ I (in hoa) và chữ l (in thường).
- **Luôn viết Hoa:** `ap-fdp-nvy` nhìn rất rối mắt. Luôn ép kiểu `toUpperCase()` trước khi lưu vào database.
- **Độ dài cố định hoặc có trần tối đa:** Không nên để SKU dài quá 20 ký tự vì nhân viên kho sẽ không in vừa lên con tem mã vạch (Barcode) dán trên túi ni-lông đựng áo.
