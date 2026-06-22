# 🛒 TÀI LIỆU ĐẶC TẢ NGHIỆP VỤ: MODULE GIỎ HÀNG (BUỘC LOGIN)

## 🧠 1. QUY TẮC KIỂM TRA TỒN KHO & GIỚI HẠN (INVENTORY & LIMIT RULES)

Đây là "trái tim" của logic tính toán số lượng khả dụng:

- **Trường hợp 1: Hàng vô hạn (Always in stock)**
  - Điều kiện: `stock_quantity = -1`.
  - Giới hạn mua tối đa: Mặc định là **99** sản phẩm/đơn hàng (hoặc lấy theo `max_order_quantity` nếu giá trị đó nhỏ hơn 99).
  - Trạng thái hiển thị: Luôn là "Còn hàng".

- **Trường hợp 2: Hàng hữu hạn (Finite stock)**
  - Điều kiện: `stock_quantity >= 0`.
  - Số lượng khả dụng thực tế: `Available = stock_quantity - locked_stock`.
  - Giới hạn mua tối đa: `Min(Available, max_order_quantity)`.
  - Trạng thái hiển thị: "Hết hàng" nếu `Available <= 0`, ngược lại là "Còn hàng".

## 🛡️ 2. LUỒNG THÊM/CẬP NHẬT GIỎ HÀNG (ADD/UPDATE ITEM)

1.  **Xác định Giới hạn (Threshold Calculation):**
    - Nếu `stock = -1`: `Limit = 99`.
    - Nếu `stock >= 0`: `Limit = stock - locked`.
    - Nếu `max_order_quantity > 0`: `Limit = Min(Limit, max_order_quantity)`.

2.  **Xác thực số lượng yêu cầu (requested_quantity):**
    - Nếu `requested_quantity > Limit`: Ném `InsufficientStockException` (nếu do kho) hoặc `MaxOrderQuantityExceededException` (nếu do giới hạn mua).

3.  **Xác thực Trạng thái Thực (Integrity Check):**
    - Nếu Sản phẩm bị Admin khóa (`status = 'BANNED'`) hoặc Shop bị khóa: Chặn tuyệt đối, không cho thêm vào giỏ.

## 👁️ 3. LUỒNG HIỂN THỊ GIỎ HÀNG & SOFT DISABLE (GET CART)

- **Logic hiển thị:**
  - Sản phẩm hợp lệ: `status = 'ACTIVE'`, Shop NORMAL, và số lượng yêu cầu $\le$ Tồn kho/Giới hạn.
  - Sản phẩm vô hiệu hóa (Disabled):
    - Lý do 1: `status = 'BANNED'` -> Hiện nhãn "Vi phạm chính sách".
    - Lý do 2: `stock = 0` (và không phải -1) -> Hiện nhãn "Hết hàng".
    - Lý do 3: Shop bị khóa -> Hiện nhãn "Shop tạm ngưng hoạt động".

- **Logic tính tiền:**
  - Tổng tiền = $\sum (Price \times Quantity)$ của các sản phẩm **Hợp lệ** và **Được tích chọn**.

---

## 🕹️ MÔ PHỎNG LOGIC KIỂM TRA GIỎ HÀNG (SIMULATOR)

Bạn có thể điều chỉnh các thông số `stock_quantity` (thử nhập -1), `max_order_quantity` và trạng thái Shop để xem hệ thống phản ứng thế nào.

---

## 🛡️ TỔNG HỢP EXCEPTIONS CẦN TRIỂN KHAI

| Tên Exception                         | Điều kiện kích hoạt (Trigger)                                       | Message gợi ý                                   |
| :------------------------------------ | :------------------------------------------------------------------ | :---------------------------------------------- |
| **InsufficientStockException**        | `stock >= 0` AND `request > stock`                                  | "Số lượng trong kho không đủ (Chỉ còn [X])."    |
| **MaxOrderQuantityExceededException** | `request > 99` (nếu `stock = -1`) OR `request > max_order_quantity` | "Bạn chỉ được mua tối đa [Limit] sản phẩm này." |
| **ProductBannedException**            | `status = 'BANNED'`                                                 | "Sản phẩm đã bị khóa do vi phạm chính sách."    |
| **SellerSuspendedException**          | `operational_status = 'BANNED'`                                     | "Cửa hàng hiện đang bị tạm dừng hoạt động."     |

**Lời khuyên từ Senior:** Khi triển khai code Java, tại hàm `addToCart`, bạn nên xử lý theo thứ tự:

1. Check Status (Banned?) -> 2. Check Inventory Type (-1 hay >=0) -> 3. Check Max Limit -> 4. Save to DB.
