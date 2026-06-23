### **📦 KỊCH BẢN 1: QUẢN TRỊ VIÊN XÁC NHẬN ĐƠN & GỌI GHN**

**Business Logic:**

1. Đơn hàng đang ở trạng thái PENDING (Chờ xác nhận).
2. Seller bấm "Chuẩn bị hàng". Hệ thống khóa dòng dữ liệu đơn hàng lại (Pessimistic Locking).
3. Backend gọi API POST /v2/shipping-order/create của GHN.
4. Nếu GHN trả về order_code thành công, Backend lưu mã này vào cột tracking_code trong bảng orders và chuyển trạng thái đơn sang AWAITING_SHIPMENT (Chờ lấy hàng).

**Từ điển Exception:**

| Tên Exception                   | Ngữ cảnh ném lỗi (Trigger)                                    | Cách xử lý / Trả về FE                                                                              |
| ------------------------------- | ------------------------------------------------------------- | --------------------------------------------------------------------------------------------------- |
| **OrderStateConflictException** | Seller bấm xác nhận nhưng khách đã hủy đơn trước đó 1 giây.   | "Đơn hàng này đã bị người mua hủy. Không thể xác nhận." (Refresh lại list).                         |
| **GHNOrderCreationException**   | API GHN báo lỗi (Sai địa chỉ, quá cân nặng, hoặc server sập). | "Lỗi kết nối đơn vị vận chuyển: \[Lý do từ GHN\]. Vui lòng thử lại." Trạng thái giữ nguyên PENDING. |

---

### **❌ KỊCH BẢN 2: NGƯỜI MUA HUỶ ĐƠN HÀNG**

**Business Logic**:Khách hàng chỉ được phép hủy đơn tự động khi trạng thái là PENDING (Shop chưa gọi GHN).

1. Khách bấm hủy, chọn lý do (Ví dụ: "Đổi ý").
2. Đổi trạng thái sang CANCELLED.
3. **Logic Kho:** Nhả locked_stock trả lại cho stock_quantity.
4. **Logic Tiền:** Nếu khách chọn thanh toán VNPAY/MoMo và đã thanh toán, gọi API Refund của cổng thanh toán để trả tiền về thẻ khách.

**Từ điển Exception:**

| Tên Exception                       | Ngữ cảnh ném lỗi (Trigger)                                      | Cách xử lý / Trả về FE                                                                               |
| ----------------------------------- | --------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------- |
| **CancellationNotAllowedException** | Đơn đã sang AWAITING_SHIPMENT (Shop đã gói hàng) hoặc SHIPPING. | "Shop đang chuẩn bị hàng, bạn không thể tự hủy. Vui lòng chat với Shop."                             |
| **PaymentRefundException**          | Gọi API hoàn tiền VNPAY thất bại.                               | Chuyển đơn sang trạng thái REFUND_FAILED. Bắn noti cho Admin vào xử lý hoàn tiền tay. (Vẫn nhả kho). |
