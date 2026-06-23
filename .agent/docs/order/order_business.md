# Bussiness Order

Mỗi dòng trong `order_details` lưu giá trị của từng món hàng đơn lẻ: `total_money` (ở bảng chi tiết) = `num_of_product` × `order_detail_price`.

Khi khách bấm đặt hàng, Backend sẽ Sum (cộng tổng) tất cả các `total_money` của bảng `order_details` lại để tạo thành trường `total_product_money` ở bảng `orders`.

Sau đó Backend mới lấy con số tổng này trừ tiếp đi các tầng discount để ra `final_payment_money`. Công thức kiểm toán chuẩn dưới Database sẽ luôn luôn là:

`final_payment_money` = (`total_product_money` - `shop_discount_amount`) + (`original_shipping_fee` - `shipping_discount_amount`) - `payment_discount_amount`

## Kịch bản xử lý lỗi luồng dữ liệu (Rất Quan Trọng)

Đối với thanh toán online qua VNPAY, khách hàng có thể gặp tình huống: **Bấm đặt hàng -&gt; Hệ thống trừ số lượng coupon trong DB -&gt; Chuyển sang trang VNPAY -&gt; Khách đổi ý không thanh toán / Tắt trình duyệt.** Nếu bạn không xử lý, coupon của khách sẽ bị kẹt hoặc bị mất. Quy trình chuẩn sẽ như sau:

1. **Khi khách bấm "Thanh toán qua VNPAY":**
   - Bạn tạo đơn hàng trong DB với trạng thái `order_status = 'PENDING_PAYMENT'`.
   - Bạn ghi nhận việc sử dụng mã vào bảng `coupon_usages` để **giữ chỗ (Hold)**, tránh việc khách mở tab khác dùng tiếp mã đó.

2. **Xử lý kết quả trả về từ VNPAY qua API IPN (Webhook):**
   - **Trường hợp thành công:** VNPAY gọi về Webhook của bạn báo thanh toán thành công -&gt; Cập nhật `order_status = 'PAID'`, gọi tiếp API GHN để đẩy đơn đi giao.
   - **Trường hợp thất bại / Hết hạn thanh toán (Sau 15 phút không trả tiền):** Bạn chạy một Worker tự động cập nhật `order_status = 'CANCELLED'` -&gt; Đồng thời tiến hành **Xóa các dòng tương ứng trong bảng coupon_usages** và hoàn tác lại số lượng `max_usage` trong bảng `coupons` để trả lại quyền lợi cho khách hàng.

## 1. Bản chất dòng chảy dữ liệu (Data Flow) khi đặt hàng

Bạn **không gửi mã coupon** sang GHN hay VNPAY. Thay vào đó, bạn tính toán số tiền giảm trước tại Backend, sau đó gửi số tiền sau giảm giá (Số tiền thực tế cần thu) sang cho họ.

```text
[FE: Bấm Đặt Hàng]
       │
       ▼
[BE: Tính toán & Khấu trừ Coupon dưới DB]
       │
       ├─► (Gửi phí ship thực tế thu hộ) ──► [API GHN]
       └─► (Gửi tổng số tiền cuối cùng) ──► [API VNPAY]
```

2\. Quy trình xử lý chi tiết cho từng tầngTầng 1: Coupon Vận Chuyển (Kết nối với GHN)\
\
Khi khách hàng chọn địa chỉ, Backend của bạn sẽ gọi API GHN (`/shipping-order/fee`) để lấy phí vận chuyển gốc (Ví dụ: GHN báo phí là **35.000đ**).

1. **Tại Backend của bạn:**
   - Hệ thống kiểm tra thấy khách có áp mã `FREESHIP_20K` (giảm 20.000đ phí ship).
   - Backend tính toán:
     - Phí ship gốc: 35.000đ
     - Số tiền giảm: 20.000đ
     - Khách thực trả: 35.000 - 20.000 = 15.000đ.
2. **Gửi sang GHN khi tạo đơn giao hàng (**`/shipping-order/create`**):**
   - Đối với GHN, số tiền khách thực trả cho ship không ảnh hưởng đến số tiền bạn trả cho GHN (bạn vẫn trả cho GHN 35.000đ phí dịch vụ cuối tháng). Cái GHN cần biết là nếu đơn này là COD, chúng ta nhờ GHN **thu hộ (COD amount)** bao nhiêu tiền từ khách.
   - Tham số gửi sang GHN:
     - `cod_amount` = Tổng tiền hàng + 15.000đ (Phí ship sau giảm).

Tầng 2: Coupon Thanh Toán (Kết nối với VNPAY)\
\
VNPAY chỉ quan tâm đến một thứ duy nhất: **Tổng số tiền mà khách hàng phải quẹt thẻ/quét mã là bao nhiêu.**

1. **Tại Backend của bạn:**
   - Giả sử: Tổng tiền hàng sau khi trừ mã giảm giá của Shop là **500.000đ**. Tiền ship khách trả (sau khi trừ mã freeship) là **15.000đ**. Khách áp thêm mã giảm giá từ ví điện tử `VNPAY_10K` (giảm 10.000đ).
   - Backend tính Tổng số tiền cuối cùng khách phải trả:\
     $$500.000 + 15.000 - 10.000 = 505.000đ$$
2. **Gửi sang VNPAY khi tạo URL thanh toán:**
   - Bạn gọi API VNPAY tạo link thanh toán với tham số `vnp_Amount = 505000 * 100` (VNPAY yêu cầu nhân 100 để bỏ dấu thập phân, tức là truyền 50500000).
