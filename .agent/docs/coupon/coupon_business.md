# Business Coupon Design

## 1. NGHIỆP VỤ QUẢN LÝ & TẠO MÃ (MANAGEMENT LOGIC)

Đây là các quy tắc khi Admin hoặc Seller thực hiện hành động "Tạo mã" trong trang quản trị. _DB không trừ số lượng trong coupons, mà đếm số bản ghi coupon_usages.used_at để tính lượt đã dùng._

### A. Phân biệt chủ thể (Ownership)

- **Mã của Sàn (Admin Coupon):** Trường `seller_id` bắt buộc phải là NULL. Mã này có quyền áp dụng lên **Tổng giá trị đơn hàng** (tổng của tất cả các shop trong giỏ).
- **Mã của Shop (Seller Coupon):** Trường `seller_id` phải trỏ đúng vào ID của Shop đang đăng nhập. Mã này chỉ có quyền áp dụng lên **Tổng giá trị sản phẩm thuộc Shop đó**.

### B. Ràng buộc khi thiết lập (Constraint Rules)

- **Định dạng Code:** Phải được `toUpperCase()` và xóa khoảng trắng. Chỉ cho phép ký tự chữ và số.
- **Loại giảm giá (Discount Type):**
  - FIXED: Giảm số tiền cụ thể (VD: 20.000đ). Số tiền giảm phải nhỏ hơn `min_order_amount`.
  - PERCENT: Giảm theo %. Phải nằm trong khoảng 1% - 100%. **Bắt buộc** phải nhập `max_discount_amount`.
- **Thời hạn:** Ngày bắt đầu phải >= Ngày hiện tại. Ngày kết thúc phải &gt; Ngày bắt đầu ít nhất 1 giờ.

---

## 2. NGHIỆP VỤ ÁP DỤNG & TÍNH TOÁN (CALCULATION LOGIC)

Đây là logic chạy ngầm khi User nhập mã ở trang Giỏ hàng/Thanh toán.

### A. Xác thực điều kiện (Validation Step)

1. **Kiểm tra tồn tại:** Mã có trong DB không? Có đang `is_active = true` không?
2. **Kiểm tra thời gian:** `start_date` <= Now <= `end_date`.
3. **Kiểm tra lượt dùng (Usage Limit):** `count(coupon_usages) < max_usage`.
4. **Kiểm tra lượt dùng cá nhân:** User này đã dùng mã này chưa? (Thường là 1 lần/user).
5. **Kiểm tra giá trị tối thiểu (Min Spend):**
   - Mã Seller: Tổng tiền của Shop đó >= `min_order_amount`.
   - Mã Admin: Tổng tiền cả giỏ hàng >= `min_order_amount`.

### B. Tính toán số tiền giảm (Calculation Step)

- **Công thức %:** `amount_discount = min(total_amount * percent, max_discount_amount)`.
- **Ưu tiên áp dụng:** Thông thường, hệ thống sẽ tính giảm giá của **Seller Coupon trước**, sau đó mới lấy con số đã giảm đó để tính tiếp giảm giá của **Admin Coupon**. (Tránh việc Sàn và Shop cùng giảm trên một con số lớn, gây lỗ).

---

## 3. HỆ THỐNG EXCEPTIONS CHI TIẾT (BẪY LỖI)

Bạn cần khai báo các Custom Exceptions sau để trả về mã lỗi HTTP 400 (Bad Request) cho Frontend:

| Tên Exception                       | Ngữ cảnh sử dụng (Trigger)                                | Thông báo trả về                                          |
| ----------------------------------- | --------------------------------------------------------- | --------------------------------------------------------- |
| **CouponNotFoundException**         | Mã không tồn tại hoặc đã bị xóa (`is_active=false`).      | "Mã giảm giá không hợp lệ."                               |
| **CouponExpiredException**          | Ngoài khoảng thời gian hiệu lực.                          | "Mã giảm giá đã hết hạn hoặc chưa đến giờ sử dụng."       |
| **CouponExhaustedException**        | Tổng lượt sử dụng trên hệ thống đã hết.                   | "Rất tiếc, mã giảm giá này đã hết lượt dùng."             |
| **CouponUsageLimitException**       | User hiện tại đã từng sử dụng mã này rồi.                 | "Bạn đã sử dụng mã này cho một đơn hàng trước đó."        |
| **InadequateOrderValueException**   | Giá trị đơn hàng chưa đạt `min_order_amount`.             | "Đơn hàng chưa đạt giá trị tối thiểu để áp dụng mã này."  |
| **InvalidCouponOwnershipException** | Seller cố tình sửa/xóa mã của Seller khác hoặc của Admin. | "Bạn không có quyền thao tác trên mã giảm giá này."       |
| **BudgetExceededException**         | Khi tạo mã PERCENT mà không có `max_discount_amount`.     | "Vui lòng thiết lập mức giảm tối đa để bảo vệ ngân sách." |

---

## 4. Lưu ý triển khai & Lời khuyên kỹ thuật

- **API áp dụng Coupon chỉ nên trả về kết quả tính toán tạm thời.** Việc thực sự "trừ lượt dùng" và lưu vào bảng `coupon_usages` chỉ được thực hiện khi User bấm nút **"Đặt hàng" (Checkout)** thành công.
- Khi đặt đơn thành công mới `insert` `coupon_usages`.
- Sử dụng `lock` để chống race-condition “hết lượt dùng” khi nhiều user checkout cùng lúc.
- `apply-preview` ở bước giỏ hàng/checkout chỉ để tính toán tạm thời.
- Sau khi đơn hàng tạo thành công (có `orderId`), gọi `commit-usages` để ghi `coupon_usages`.

**Khi “mã đang được dùng”:**

- Đơn đã commit usage rồi: không bị ảnh hưởng, vì đã có bản ghi `coupon_usages`.
- User mới nhập mã sau khi bị tắt: fail ngay (mã không hợp lệ/không active).
- User đã preview mã nhưng chưa commit đơn: có thể fail ở bước commit nếu mã bị tắt giữa chừng.
- Nếu commit đang chạy đúng lúc admin tắt mã: nhờ lock, 1 bên sẽ chờ; thường đơn đang commit có thể hoàn tất, sau đó mã bị tắt cho các đơn tiếp theo.

**Về hành động “xóa”:**

- Bạn đang không xóa cứng, mà vô hiệu hóa (soft delete vĩnh viễn), nên không làm mất lịch sử đối soát.
- Nếu bạn muốn UX mượt hơn, bước tiếp theo nên là trả lỗi business riêng kiểu: “Mã đã bị ngừng áp dụng, vui lòng chọn mã khác” ở bước commit để FE hiển thị rõ lý do.

**Các giới hạn bổ sung:**

- Thêm điều kiện mức khuyến mãi không vượt quá 69% giá trị đơn hàng tối thiểu.
- Giá trị coupon tối đa không vượt quá 120.000.000.
- Số lượt tối đa mỗi người mua không quá 5 lượt.

**Về quy tắc Multi-shop:**

- Hiện tại logic cho phép dùng coupon sàn + coupon shop cùng lúc nhưng chỉ khi checkout 1 shop.
- Lý do chặn multi-shop: không có cơ chế “phân bổ coupon admin” cho nhiều order (phải chia discount theo từng shop), nên dễ sai số tiền và sai cập nhật `discount_amount`.
  - Cho phép: 1 shop + (coupon shop) + (coupon sàn)
  - Không cho phép: nhiều shop + coupon (sàn)

### 5. Đổi logic Rollback Coupon sang deleteByOrderId(...)

- **Nguyên nhân:** Trước đây, hệ thống giữ chỗ (reserve) coupon bằng cách insert vào bảng `coupon_usages` với `used_at = NULL`. Nếu đơn COD hủy, hàm rollback cũ tìm record NULL để xóa, nhưng với COD hiện tại bạn đã commit (có `used_at` thời gian thực), nên hàm rollback cũ bỏ qua -> Lỗi giam mã giảm giá.
- **Cách giải quyết:** Dùng `couponUsageRepository.deleteByOrderId(orderId)` là "nhát chém" dứt khoát. Bất kể coupon đó đang ở trạng thái Pending hay Committed, cứ hủy đơn `orderId` đó thì xóa sạch lịch sử dùng mã.

---

## 6. Cách làm tính năng "Tự động gợi ý & Áp dụng coupon" theo thời gian thực

Quy trình này không nên chạy thụ động (chờ bấm nút), mà phải tự động kích hoạt mỗi khi có sự thay đổi từ giỏ hàng.

### Bước 1: Frontend bắt sự kiện (Trigger)

Mỗi khi khách hàng thực hiện các hành động trên giao diện (Tích chọn/Bỏ chọn sản phẩm, thay đổi số lượng, thay đổi địa chỉ), FE sẽ gom toàn bộ thông tin giỏ hàng (Checkout Context) và gửi request `POST /api/v1/checkout/calculate` hoặc `/api/v1/coupons/suggest`.

### Bước 2: Backend xử lý "Lọc và Thử nghiệm" (Validation Engine)

Backend chạy hàm quét toàn bộ coupon đang hoạt động:

```sql
SELECT * FROM coupons
WHERE is_active = TRUE
  AND NOW() BETWEEN start_date AND end_date
  AND max_usage > 0;
```

Sau đó, Backend chạy vòng lặp để giả lập áp dụng dựa trên:

1. **Kiểm tra điều kiện cứng:** Đơn hàng có đạt `min_order_amount` không? Đã dùng quá `user_usage_limit` chưa?
2. **Kiểm tra điều kiện nâng cao (JSONB):** Check các điều kiện (ví dụ: áp dụng cho danh mục nào).
3. **Tính số tiền được giảm:** Với mỗi mã hợp lệ, tính ra số tiền giảm cụ thể ($X$ VNĐ).

### Bước 3: Phản hồi về FE

Backend trả về 2 nhóm:

- `available_coupons`: Các mã thỏa mãn, kèm tiền giảm. Tự động đánh dấu `is_best = true` cho mã tiết kiệm nhất.
- `unavailable_coupons`: Các mã không thỏa mãn (kèm gợi ý mua thêm).

---

## 7. Áp dụng CHỒNG MÃ và Cải tiến Database

Thực tế các sàn TMĐT cho phép áp dụng chồng nhiều mã nếu phân chia theo 3 Tầng Khuyến Mãi độc lập:

### 3 Tầng Coupon trên thực tế:

1. **Tầng 1: Mã giảm giá của Shop (Shop Voucher):** Giảm trên tổng giá trị tiền hàng.
2. **Tầng 2: Mã giảm giá vận chuyển (Free Shipping Voucher):** Trừ thẳng vào phí ship.
3. **Tầng 3: Mã giảm giá của Sàn / Đối tác thanh toán (Platform / Payment Voucher):** Giảm trên tổng số tiền sau cùng.

_Quy tắc:_ Trong cùng một tầng chỉ chọn loại tối ưu nhất. Khác tầng được áp dụng chồng lên nhau.

### Cải tiến Database

Thêm cột `coupon_type` vào bảng `coupons`:

```sql
ALTER TABLE coupons ADD COLUMN coupon_type VARCHAR(30) NOT NULL DEFAULT 'SHOP_VOUCHER';
-- Các giá trị: 'SHOP_VOUCHER' (Tiền hàng), 'FREE_SHIPPING' (Phí ship), 'PAYMENT_VOUCHER' (Cổng thanh toán)
```

**Thuật toán Backend tự động chọn mã tốt nhất:**

1. **Xử lý tầng SHOP_VOUCHER:** Lọc các mã hợp lệ, tính mã giảm nhiều nhất -> Tự động chọn làm mặc định.
2. **Xử lý tầng FREE_SHIPPING:** Tương tự, chọn mã giảm phí ship tốt nhất.
3. **Xử lý tầng PAYMENT_VOUCHER:** Tương tự, chọn mã thanh toán hợp lệ (ví dụ theo MoMo).
4. **Cộng tổng:** Trả về cho FE tổng tiền giảm = Mã Shop tốt nhất + Mã Freeship tốt nhất + Mã Ví tốt nhất.
