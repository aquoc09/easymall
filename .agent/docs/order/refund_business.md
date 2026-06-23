# Business refund

**Đơn hàng mới tinh (PENDING CONFIRMATION), khách vừa đặt xong, nhưng Shop check lại thấy hết hàng (hoặc sai giá) nên tự vào bấm Hủy luôn (Chưa hề bấm nút "Chuẩn bị hàng").**

Tuy nhiên, bài toán **Tiền bạc (COD vs Online)** thì vẫn phải xử lý cực kỳ cẩn thận. Dưới đây là Đặc tả Business Logic và Exception cho kịch bản này:

---

### 🧠 BƯỚC 1: KIỂM TRA ĐIỀU KIỆN (VALIDATION)

- **Quyền sở hữu:** order.seller_id phải khớp với ID của Seller đang thao tác và nhân viên shop đó.
- **Trạng thái hợp lệ:** Đơn hàng BẮT BUỘC phải đang ở trạng thái PENDING CONFIRMATION (Chờ xác nhận). Nếu đơn đã sang CONFIRMED hoặc cao hơn, ném lỗi ngay lập tức.

---

### 💰 BƯỚC 2: XỬ LÝ THANH TOÁN & HOÀN TIỀN (CORE LOGIC)

Không gọi GHN, chúng ta đi thẳng vào xử lý dòng tiền. Tùy vào payment_method mà rẽ nhánh:

**Kịch bản 2A: Thanh toán COD (Tiền mặt)**

- **Tình trạng:** Khách chọn trả tiền mặt, hệ thống chưa thu đồng nào (payment_status = 'UNPAID').
- **Hành động:**
  1. Chỉ cần update DB: Đổi status = 'CANCELLED'.
  2. Bỏ qua hoàn tiền.

**Kịch bản 2B: Thanh toán ONLINE (VNPAY / MoMo)**

- **Tình trạng:** Khách đã thanh toán qua cổng điện tử, tiền đã chui vào tài khoản trung gian của Sàn (payment_status = 'PAID').
- **Hành động:**
  1. **Gọi API:** Backend gọi API Refund của VNPAY/MoMo/stripe để trả tiền lại vào tài khoản ngân hàng của khách.
  2. **Ghi Log:** Insert 1 dòng vào bảng transactions (ID của Shop, amount = total_amount, type = 'REFUND').
  3. **Update DB:** Chuyển status = 'CANCELLED' và payment_status = 'REFUNDED'.

---

### 📦 BƯỚC 3: TRẢ LẠI TÀI NGUYÊN & PHẠT SHOP

Khách không mua được thì phải nhả đồ ra cho người khác mua:

1. **Hoàn Tồn kho:** Trừ locked_stock và cộng trả lại cho stock_quantity trong bảng product_variants (Cần dùng Pessimistic Lock ở đây để tránh sai số).
2. **Hoàn Mã giảm giá:** Xóa record trong coupon_usages để trả lại lượt dùng cho khách.
3. **Ghi nhận Lỗi của Shop:** UPDATE seller_stats SET cancelled_by_seller_count = cancelled_by_seller_count + 1. (Dù là hủy sớm, việc Shop tự hủy đơn vẫn là trải nghiệm tồi tệ cho khách, Sàn phải đếm số lần này để cảnh cáo Shop nếu vi phạm nhiều).
4. **Thông báo:** Gửi Noti báo cho khách hàng biết đơn đã bị Shop hủy.

---

### 🛡️ TỪ ĐIỂN EXCEPTIONS (BẪY LỖI) CHO API

| Tên Exception (Java Class)     | HTTP Code       | Ngữ cảnh ném lỗi (Trigger)                                                                    | Message trả về Frontend                                                                       |
| ------------------------------ | --------------- | --------------------------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------- |
| **OrderOwnershipException**    | 403 Forbidden   | Seller ID hoặc nhân viên thao tác không phải là chủ của đơn hàng.                             | _"Bạn không có quyền thao tác trên đơn hàng này."_                                            |
| **InvalidOrderStateException** | 400 Bad Request | Trạng thái hiện tại không phải là PENDING_CONFIRMATION.                                       | _"Chỉ có thể hủy đơn hàng đang ở trạng thái Chờ xác nhận."_                                   |
| **PaymentRefundException**     | 502 Bad Gateway | (Dành cho Online) Gọi API VNPAY/MoMo/stripe bị lỗi, timeout hoặc ngân hàng từ chối hoàn tiền. | _"Lỗi cổng thanh toán: Không thể tự động hoàn tiền cho khách. Vui lòng thử lại sau ít phút."_ |
