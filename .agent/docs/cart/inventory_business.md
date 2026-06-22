# 📦 TÀI LIỆU NGHIỆP VỤ: QUẢN LÝ TỒN KHO (INVENTORY MANAGEMENT)

## 1. Công Thức Lõi: Tồn Kho Khả Dụng (Available Stock)

Hệ thống **không bao giờ** được phép dùng `stock_quantity` để kiểm tra trực tiếp xem khách có mua được hàng hay không. Hệ thống BẮT BUỘC phải sử dụng công thức:

> `Available Stock = stock_quantity - locked_stock`

- **`stock_quantity` (Tồn kho thực tế):** Số lượng hàng vật lý đang nằm trong kho của Seller. (Trừ trường hợp `= -1` là tồn kho vô cực/bán order).
- **`locked_stock` (Tạm giữ):** Số lượng hàng khách đã bấm "Đặt hàng" thành công nhưng đơn hàng chưa hoàn tất (đang Chờ xác nhận, Đang giao, v.v.).

---

## ⚙️ 2. Nghiệp Vụ: Lúc Seller Tạo / Sửa Sản Phẩm

Khi sử dụng API Tạo/Sửa Sản Phẩm (`POST`/`PUT` `/products`), trạng thái sẽ được tự động hóa hoàn toàn. Seller CHỈ CẦN nhập số lượng vào cột `stock_quantity` của từng biến thể (Variant).

**Bước 1: Tính toán `inventory_status` cho TỪNG BIẾN THỂ (Variant):**

- Nếu `stock_quantity == -1` (Vô cực) ➔ Set `inventory_status = true`.
- Nếu `stock_quantity > 0` ➔ Set `inventory_status = true`.
- Nếu `stock_quantity == 0` ➔ Set `inventory_status = false`.

**Bước 2: Tính toán `inventory_status` cho SẢN PHẨM CHA (Product):**

- Nếu **CÓ ÍT NHẤT 1** biến thể có `inventory_status == true` ➔ Sản phẩm cha gán `inventory_status = true` (Mặt hàng này vẫn còn bán được).
- Nếu **TẤT CẢ** các biến thể đều có `inventory_status == false` ➔ Sản phẩm cha gán `inventory_status = false` (Cạn sạch kho toàn bộ màu/size).

> **🔥 Điểm ăn tiền về Elasticsearch:**
> Sản phẩm Hết hàng (Out of stock) **KHÔNG BỊ CHUYỂN TRẠNG THÁI** sang `INACTIVE`. Nó vẫn là `ACTIVE` và được đẩy lên Elasticsearch.
> _Lý do:_ Để giữ SEO (link Google không bị chết) và khách hàng vẫn search ra. Giao diện Frontend sẽ dán chữ "Hết hàng" lên ảnh, làm mờ nút Mua, và Elasticsearch tự động xếp sản phẩm này xuống cuối danh sách tìm kiếm.

---

## 🛒 3. Nghiệp Vụ: Lúc Khách Hàng Mua Sắm (Checkout)

Đây là lúc `locked_stock` phát huy tác dụng bảo vệ hệ thống:

**Hành động 1: Khi khách bấm "Đặt Hàng" (Place Order)**

- Hệ thống tính: `Available Stock = stock_quantity - locked_stock`.
- Nếu `Available Stock >= Số lượng khách mua` ➔ Cho phép tạo đơn.
- Chạy lệnh Database _(Lưu ý: Bỏ qua nếu `stock_quantity == -1`)_:

```sql
UPDATE product_variants
SET locked_stock = locked_stock + {số_lượng_mua}
WHERE variant_id = ?;

```

**Hành động 2: Khi đơn hàng Giao Thành Công (Delivered)**

- Hàng đã đến tay khách, kho vật lý thực sự mất đi.
- Chạy lệnh:

```sql
UPDATE product_variants
SET stock_quantity = stock_quantity - {số_lượng_mua},
    locked_stock = locked_stock - {số_lượng_mua}
WHERE variant_id = ?;

```

**Hành động 3: Khi đơn hàng Bị Hủy (Canceled/Returned)**

- Khách boom hàng hoặc Admin hủy đơn. Hàng vật lý không mất đi.
- Chạy lệnh trả kho:

```sql
UPDATE product_variants
SET locked_stock = locked_stock - {số_lượng_mua}
WHERE variant_id = ?;

```

---

## 🛡️ 4. Cập Nhật Exception Cho Tầng Service

Bổ sung 2 bẫy lỗi (Nhóm `400 Bad Request`) vào hệ thống:

- **`InvalidStockQuantityException`**: Ném ra ở luồng Seller Tạo/Cập nhật sản phẩm.
- _Message:_ "Số lượng tồn kho phải là số nguyên dương hoặc -1 (Tồn kho vô cực)."

- **`OutOfStockException`**: Ném ra ở luồng Khách hàng thêm vào giỏ hoặc Đặt hàng.
- _Message:_ "Sản phẩm [Tên sản phẩm - Phân loại] hiện chỉ còn [X] sản phẩm khả dụng. Vui lòng giảm số lượng mua."

---

## 💡 5. Các Bài Toán Thực Tế Và Cách Giải Quyết

### 🛡️ Bài toán 1: "Giam hàng" (Inventory Hoarding / Abandoned Cart)

- **Nỗi đau:** Đối thủ tạo tài khoản ảo, cho hết sản phẩm "Hot" vào giỏ rồi bấm Đặt hàng nhưng không nhận/không thanh toán. Nếu trừ thẳng kho, shop báo "Hết hàng" và mất khách thật.
- **Giải pháp (Soft-Lock):** Khi khách bấm "Đặt hàng", kho không mất đi mà chuyển sang trạng thái tạm giữ.

```sql
UPDATE product_variants
SET stock_quantity = stock_quantity - 1,
    locked_stock = locked_stock + 1;

```

_(Ví dụ: Có 10 cái, đối thủ đặt 2. Kho cập nhật thành `stock = 8, locked = 2`. Khách sau vào vẫn thấy 8 cái)._

- **Rollback:** Nếu sau 15 phút không thanh toán (Timeout), Cronjob tự động trả hàng:

```sql
UPDATE product_variants
SET stock_quantity = stock_quantity + 2,
    locked_stock = locked_stock - 2;

```

### ⚡ Bài toán 2: "Bán lố" (Overselling) trong Mega Sale

- **Nỗi đau:** Đêm Flash Sale kho còn 1 chiếc iPhone, nhưng 1.000 người bấm mua cùng 1 mili-giây. Dễ dẫn đến `stock_quantity = -999`.
- **Giải pháp (Atomic Transaction & DB Constraints):** Không lấy tồn kho lên RAM để tính toán nhằm tránh _Race Condition_. Khóa trực tiếp bằng SQL với điều kiện `WHERE`:

```sql
UPDATE product_variants
SET stock_quantity = stock_quantity - 1
WHERE variant_id = X AND stock_quantity >= 1;

```

_Cơ chế:_ Khi 1.000 request ập đến, Database Engine sẽ xếp hàng. Request đầu tiên thành công đưa `stock` về `0`. 999 request sau sẽ thất bại do điều kiện `>= 1` không còn đúng.

### 🔄 Bài toán 3: "Thất thoát đồng bộ" khi Hoàn/Trả hàng (Return / Refund)

- **Nỗi đau:** Khách không nhận hàng, shipper trả về kho nhưng phần mềm vẫn báo trừ đi. Kế toán phải cộng tay cuối tháng.
- **Giải pháp (Webhook tự động):** Khi Shipper bấm "Trả hàng thành công", request bắn về Backend bắn trạng thái `RETURNED`. Hệ thống tự động trừ doanh thu ảo và cập nhật:

```sql
UPDATE product_variants SET stock_quantity = stock_quantity + 1;

```

---

## 📊 6. Tại Sao Dùng 2 Cột Thay Vì 1 Cột Tồn Kho?

Việc tách riêng biệt giúp giải quyết bài toán phân biệt giữa **Kho ảo (Logic Inventory)** và **Kho thật (Physical Inventory)**.

_Ví dụ: Kệ thực tế có 100 chiếc áo. Có 2 khách mua tổng 5 chiếc nhưng chưa thanh toán/chờ xác nhận._

| Tiêu chí so sánh     | Kịch bản 1: Sửa 1 cột (`stock_quantity`)                              | Kịch bản 2: Dùng 2 cột (`stock_quantity` & `locked_stock`)                  |
| -------------------- | --------------------------------------------------------------------- | --------------------------------------------------------------------------- |
| **Logic ghi nhận**   | Trừ thẳng: `stock_quantity = 95`                                      | `stock_quantity = 95`                                                       |
| **Góc nhìn Thủ kho** | Nhìn PM thấy 95, đếm kệ thấy 100 ➔ **Hoang mang, nghĩ lỗi hệ thống.** | Bán tiếp: **95**, Đang giữ chỗ: **5**. Tổng kệ: **100** ➔ **Khớp thực tế.** |
| **Rủi ro hệ thống**  | Server crash hoặc mất log không rõ tại sao số 95 bị trừ đi.           | Hệ thống và vận hành nắm rõ hàng đang nằm ở khâu nào trong chuỗi.           |
| **Đánh giá**         | Thiếu minh bạch, dễ thất thoát.                                       | **Quy chuẩn bắt buộc** của các hệ thống ERP & E-commerce lớn.               |
