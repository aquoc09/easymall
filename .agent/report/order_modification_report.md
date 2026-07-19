# Báo Cáo Thay Đổi Trong Order & Database

Dưới đây là tổng hợp chi tiết các thành phần đã được chỉnh sửa liên quan đến Order và Database để đáp ứng yêu cầu áp dụng mã giảm giá `FREE_SHIPPING` và `PAYMENT_VOUCHER`.

## 1. Thay Đổi Database (Bảng `orders`)

Để hệ thống có thể lưu vết và hiển thị rõ số tiền được giảm từ mã giảm giá của Shop (Shop Voucher) tách biệt với mã Miễn phí vận chuyển (Free Shipping), một cột mới đã được thêm vào bảng `orders`.

### Thông tin cột mới:
- **Tên cột**: `shop_discount_amount`
- **Kiểu dữ liệu**: `NUMERIC(15, 2)` (Giống với các cột tiền tệ khác như `total_product_money`).
- **Giá trị mặc định**: `0.00`
- **Cách thức thêm**: Sử dụng Flyway Migration qua file script `V6.8__alter_orders_add_shop_discount.sql`. Việc này đảm bảo tính nhất quán của Database khi deploy lên các môi trường khác nhau thay vì phụ thuộc vào Hibernate tự sinh (auto DDL).

## 2. Thay Đổi Trong Source Code Order

### A. Tầng Entity và DTO (`OrderEntity`, `OrderResponse`, `OrderMapper`)
- **`OrderEntity`**: Thêm trường `private BigDecimal shopDiscountAmount;` và map với cột mới trong DB.
- **`OrderResponse`**: Thêm field `shopDiscountAmount` để trả dữ liệu về cho Frontend. Điều này giúp Frontend có thể bóc tách hiển thị (ví dụ: Tổng tiền hàng, Phí vận chuyển, Giảm giá Shop, Giảm giá Vận chuyển, Tổng thanh toán).
- **`OrderMapper`**: Cập nhật hàm map để ánh xạ dữ liệu `shopDiscountAmount` từ Entity sang Response.

### B. Logic Thanh Toán (`OrderServiceImpl`)
Quy trình thanh toán (`checkout`) đã được thiết kế lại thứ tự thực thi để đảm bảo tính chính xác cho các loại mã giảm giá:

1. **Đảo ngược thứ tự tính phí vận chuyển GHN**: 
   - **Trước đây**: Hệ thống tính Voucher trước, sau đó mới tính phí ship GHN. Điều này làm cho mã `FREE_SHIPPING` bị lỗi do chưa có thông tin phí ship lúc validate.
   - **Hiện tại**: Hệ thống sẽ gọi API GHN để tính `originalShippingFee` **TRƯỚC** khi áp dụng Coupon.

2. **Truyền ngữ cảnh (Context) vào tính toán Coupon**:
   - Trong vòng lặp xử lý các mã giảm giá (`request.getCouponCodes()`), `OrderServiceImpl` giờ đây truyền thêm hai tham số quan trọng vào `CouponService`:
     - `request.getPaymentMethod()`: Để validate mã `PAYMENT_VOUCHER` xem phương thức thanh toán User chọn có hợp lệ không.
     - `originalShippingFee`: Để `calculateDiscount` biết chính xác phí ship gốc để giảm giá cho mã `FREE_SHIPPING`.

3. **Lưu trữ riêng biệt số tiền giảm giá**:
   - Tiền giảm từ `SHOP_VOUCHER` được cộng dồn vào `shopDiscountAmount`.
   - Tiền giảm từ `FREE_SHIPPING` được cộng dồn vào `shippingDiscountAmount`.
   - Tiền giảm từ `PAYMENT_VOUCHER` được cộng dồn vào `paymentDiscountAmount`.
   - Cuối cùng, `OrderEntity` sẽ được `build()` và lưu (`save`) tất cả các thông tin này xuống Database một cách hoàn chỉnh.

> [!TIP]
> Việc chia nhỏ các loại Discount và lưu tách biệt giúp cho hệ thống Kế toán, Đối soát đơn hàng sau này rõ ràng hơn rất nhiều, cũng như mang lại trải nghiệm minh bạch cho User trên UI Hóa đơn.
