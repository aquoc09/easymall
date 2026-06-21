# 📑 TÀI LIỆU GIAO TIẾP BE - FE: MODULE BIẾN THỂ SẢN PHẨM

Frontend cần phải phân biệt rõ 2 khái niệm cốt lõi gửi xuống Backend:

1. **options_config (Cấu hình phân loại):** Nằm ở Product cha. Nó định nghĩa CÁC NHÓM phân loại (Ví dụ: Màu sắc, Kích cỡ).
2. **variants (Danh sách biến thể chốt hạ):** Từng mặt hàng cụ thể sinh ra từ cấu hình trên (Ví dụ: Áo Đỏ - Size M).

Dưới đây là 2 kịch bản JSON mà FE PHẢI tuân thủ:

## 🟢 KỊCH BẢN 1: SẢN PHẨM CÓ NHIỀU PHÂN LOẠI (Có Options)

**Ví dụ:** Bán Áo thun có 2 màu (Đỏ, Xanh) và 2 size (M, L).

**FE phải gửi Payload JSON như sau:**

```json
{
  "name": "Áo thun nam Cực Cool",
  "category_id": 123,
  // 1. CẤU HÌNH PHÂN LOẠI (Lưu vào bảng products)
  "options_config": [
    {
      "name": "Màu sắc",
      "values": ["Đỏ", "Xanh"]
    },
    {
      "name": "Kích cỡ",
      "values": ["M", "L"]
    }
  ],
  // 2. TỔ HỢP BIẾN THỂ (Lưu vào bảng product_variants)
  // FE phải tự nhân chập (2 màu x 2 size = BẮT BUỘC có đúng 4 object ở đây)
  "variants": [
    {
      "variant_attributes": { "Màu sắc": "Đỏ", "Kích cỡ": "M" },
      "sku": "AO-DO-M",
      "price": 150000.0,
      "stock_quantity": 100,
      "image_url": "https://link-anh-ao-do.jpg"
    },
    {
      "variant_attributes": { "Màu sắc": "Đỏ", "Kích cỡ": "L" },
      "sku": "AO-DO-L",
      "price": 155000.0,
      "stock_quantity": 50,
      "image_url": "https://link-anh-ao-do.jpg"
    },
    {
      "variant_attributes": { "Màu sắc": "Xanh", "Kích cỡ": "M" },
      "sku": "AO-XANH-M",
      "price": 150000.0,
      "stock_quantity": 0, // Hết hàng
      "image_url": "https://link-anh-ao-xanh.jpg"
    },
    {
      "variant_attributes": { "Màu sắc": "Xanh", "Kích cỡ": "L" },
      "sku": "AO-XANH-L",
      "price": 155000.0,
      "stock_quantity": -1, // Tồn kho vô cực (Bán order)
      "image_url": "https://link-anh-ao-xanh.jpg"
    }
  ]
}
```

## 🟡 KỊCH BẢN 2: SẢN PHẨM KHÔNG CÓ PHÂN LOẠI (Sản phẩm đơn / Default Variant)

**Ví dụ:** Bán 1 cái "Cốc sứ trắng" duy nhất, không chọn màu, không chọn size.

Đây là lúc "Luật Vàng" của chúng ta phát huy tác dụng. Dù không có option, FE **BẮT BUỘC** vẫn phải gửi xuống 1 Variant mặc định.

**FE phải gửi Payload JSON như sau:**

```json
{
  "name": "Cốc sứ trắng tinh khôi",
  "category_id": 456,

  // FE gửi mảng RỖNG (Hoặc null)
  "options_config": [],

  // FE BẮT BUỘC gửi 1 object duy nhất trong mảng variants
  "variants": [
    {
      "variant_attributes": { "Default": "Default" }, // BE sẽ dựa vào đây để biết là ko có phân loại
      "sku": "COC-SU-01",
      "price": 50000.0,
      "stock_quantity": 200,
      "image_url": null // Ảnh lấy từ ảnh chung của Product cha là được
    }
  ]
}
```

## 🛑 CÁC LUẬT (BUSINESS RULES) ÉP FRONTEND PHẢI VALIDATE

Để tránh việc BE phải hứng rác và ném lỗi liên tục làm trải nghiệm Seller bị kém, bạn hãy yêu cầu team FE **phải bắt lỗi ngay trên giao diện (Client-side validation)** trước khi bấm nút Submit:

1.  **Luật Tổ Hợp Chập (Permutation Rule):** Số lượng object trong mảng variants BẮT BUỘC phải bằng Tích số lượng values trong options_config.
    - _Ví dụ:_ 3 Màu x 4 Size $\\rightarrow$ FE phải gen ra đúng 12 dòng nhập liệu. Không gửi thiếu, không gửi thừa.
2.  **Luật Giới Hạn (Anti-Spam):**
    - Tối đa **2 nhóm** options_config (Ví dụ: Màu sắc và Kích cỡ. Không cho phép thêm nhóm thứ 3 như "Chất liệu"). Shopee và Tiktok Shop cũng chỉ cho tối đa 2 nhóm.
    - Tổng số lượng variants sinh ra không được vượt quá **50**. (Nếu Seller nhập 10 Màu x 6 Size = 60 $\\rightarrow$ FE chặn ngay, báo lỗi vượt quá 50 biến thể).
3.  **Luật Giá & Kho (Pricing & Stock):**
    - price BẮT BUỘC \> 0.
    - stock_quantity BẮT BUỘC \>= 0 HOẶC Bằng -1. Các số âm khác (như -2, -5) FE phải chặn lại.
4.  **Luật Trùng Lặp SKU:**
    - Trong cùng 1 lần gửi, mã sku giữa các biến thể KHÔNG ĐƯỢC trùng nhau. (FE dùng thuật toán Set/Filter để check trùng mảng SKU trước khi gọi API).
