# FILE ĐẶC TẢ: THUẬT TOÁN DIFF ORPHAN (XỬ LÝ MỒ CÔI)

## 1. Tình huống ban đầu (Database đang lưu gì?)

Giả sử Sản phẩm có product_id = 1 đang có cấu hình Size [S, M] và Màu [Đỏ]. Database đang có 2 bản ghi trong product_variants:

- **Biến thể A:** Màu Đỏ - Size S (ID: 101) | Giá: 100k | Tồn kho: 10 | is_active = true
- **Biến thể B:** Màu Đỏ - Size M (ID: 102) | Giá: 100k | Tồn kho: 5 | is_active = true
- _Giỏ chứa ID hiện tại của DB:_ [101, 102]

## 2. Hành động của Seller (Request gửi lên)

Seller quyết định thay đổi toàn bộ dải sản phẩm:

- Ngừng bán size M (Đồng nghĩa với việc xóa ID 102).
- Bán thêm size L.
- Bán thêm màu Xanh.

Lúc này, cấu hình biến thể mới sinh ra từ tổ hợp chập (Màu [Đỏ, Xanh] x Size [S, L]) sẽ có 4 biến thể bắt buộc gửi lên Backend:

1. Đỏ - S (Đã có sẵn ở DB, mang theo ID 101, tăng giá lên 120k).
2. Đỏ - L (Mới, ID null).
3. Xanh - S (Mới, ID null).
4. Xanh - L (Mới, ID null).

### 💻 MÔ TẢ JSON PAYLOAD GỬI LÊN (PUT /api/v1/products/1)

```json
{
  "product_name": "Áo Thun Thể Thao Cao Cấp",
  "category_id": 1,
  "options_config": {
    "sizes": ["S", "L"],
    "colors": [
      { "colorName": "Đỏ", "colorCode": "#FF0000" },
      { "colorName": "Xanh", "colorCode": "#0000FF" }
    ]
  },
  "variants": [
    {
      "variant_id": 101,
      "sku_code": "TEE-RED-S",
      "price": 120000,
      "cost_price": 80000,
      "stock_quantity": 20,
      "variant_attributes": {
        "size": "S",
        "colorName": "Đỏ",
        "colorCode": "#FF0000"
      }
    },
    {
      "variant_id": null,
      "sku_code": "",
      "price": 150000,
      "cost_price": 90000,
      "stock_quantity": 50,
      "variant_attributes": {
        "size": "L",
        "colorName": "Đỏ",
        "colorCode": "#FF0000"
      }
    },
    {
      "variant_id": null,
      "sku_code": "",
      "price": 150000,
      "cost_price": 90000,
      "stock_quantity": 30,
      "variant_attributes": {
        "size": "S",
        "colorName": "Xanh",
        "colorCode": "#0000FF"
      }
    },
    {
      "variant_id": null,
      "sku_code": "",
      "price": 150000,
      "cost_price": 90000,
      "stock_quantity": 30,
      "variant_attributes": {
        "size": "L",
        "colorName": "Xanh",
        "colorCode": "#0000FF"
      }
    }
  ],
  "weight_kg": 0.3,
  "length_m": 0.2,
  "width_m": 0.15,
  "height_m": 0.03
}
```

## ⚙️ CÁCH BACKEND XỬ LÝ DIFF ORPHAN (Theo Flow Mới Nhất)

Khi Backend nhận được JSON trên, luồng code sẽ chạy qua các bước xử lý biến thể như sau:

**Bước 1: Khởi tạo cuốn sổ tay**
Hệ thống tạo một danh sách rỗng để lưu các ID được giữ lại: `retainedVariantIds = []`.

**Bước 2: Xử lý Vòng lặp For trên mảng variants**

- **Lần lặp 1 (Item có ID: 101):**
  - Phát hiện variant_id có giá trị. Thêm 101 vào mảng `retainedVariantIds`.
  - Query Database lấy số tồn kho cũ của ID 101 (VD: 10). Mức tồn mới là 20. Delta = +10.
  - Insert log ADJUSTMENT (+10) vào `torytory_transactions`.
  - Update giá (120k) và thuộc tính vào DB.
- **Lần lặp 2, 3, 4 (Các item có ID: null):**
  - Phát hiện đây là hàng mới tinh.
  - Hệ thống tự động sinh `sku_code` (nếu chuỗi rỗng) thành: AT-1-RED-L, AT-1-BLU-S, v.v.
  - Thực hiện Insert bản ghi mới vào DB.
  - Insert log IMPORT vào `inventory_transactions` tương ứng với số lượng tồn kho (50, 30, 30).

**Bước 3: Dọn rác (Deactivate Orphan)**
Kết thúc vòng lặp, cuốn sổ tay `retainedVariantIds` lúc này chỉ chứa \[101\]. Backend thực thi một câu lệnh SQL duy nhất để vô hiệu hóa tất cả các biến thể "mồ côi" (không được gửi lên nữa):

```sql
UPDATE product_variants
SET is_active = false
WHERE product_id = 1
  AND variant_id NOT IN (101);

```

_(Hệ thống sẽ âm thầm đánh cờ is_active = false cho bản ghi mang ID 102 trong Database)._

## ✨ Kết quả cuối cùng trong Database:

- (ID 101): Đỏ - S | Giá: 120k | is_active = true (Đã cập nhật)
- _(ID 102): Đỏ - M | Giá: 100k | is_active = false (Bị ẩn, nhưng dữ liệu vẫn còn để đơn hàng cũ đối chiếu)_
- (ID 103): Đỏ - L | Giá: 150k | is_active = true (Mới tạo)
- (ID 104): Xanh - S | Giá: 150k | is_active = true (Mới tạo)
- (ID 105): Xanh - L | Giá: 150k | is_active = true (Mới tạo)
