# Session Report: AI Fraud Detection Integration & Bug Fixes

**Date**: 2026-06-26
**Module**: EasyMall Core (Spring Boot) & Fraud Detection Module (FastAPI)

## 1. Overview
Phiên làm việc này tập trung vào việc hoàn thiện luồng tích hợp giữa Backend (EasyMall) và AI Module (Fraud Detection), đồng thời fix triệt để các lỗi liên quan đến bảo mật (Permission), giao tiếp API, và ánh xạ kiểu dữ liệu (Data Type Mapping) xuống cơ sở dữ liệu PostgreSQL.

## 2. Các thay đổi và Fixes chính

### 2.1. Chuẩn hóa Security & Permissions
- Thay thế toàn bộ `@PreAuthorize("hasAuthority(...)")` thành `@PreAuthorize("@permissionChecker.has(...)")` tại các Controller: `CartController`, `CouponController`, và `OrderController` để sử dụng custom permission checker.
- Sửa lỗi trong `CartServiceImpl`: Khi user lấy thông tin giỏ hàng rỗng, trả về một DTO rỗng thay vì thực hiện lệnh `INSERT` xuống Database trong context của transaction `readOnly = true`.

### 2.2. Khắc phục lỗi kết nối AI Module (404 Not Found)
- **Vấn đề**: `AiIntegrationService` của Java gọi đến endpoint `/api/ml/fraud/predict`, nhưng server FastAPI (`main.py`) lại định nghĩa router là `/api/orders/checkout`.
- **Giải pháp**: Đã chuẩn hóa lại file `main.py` bên Python để sử dụng chung endpoint `/api/ml/fraud/predict` cho phù hợp với tiêu chuẩn đặt tên của một ML Service.

### 2.3. Khắc phục lỗi Mapping JSONB trong PostgreSQL
Quá trình lưu order và đánh giá Fraud liên tục bị báo lỗi từ PostgreSQL: `ERROR: column "..." is of type jsonb but expression is of type character varying`. 
Hai Entity đã được xử lý triệt để để tận dụng tính năng hỗ trợ JSON tự động của Hibernate 6:

1. **FraudRecordEntity (`top_risk_factors`)**
   - Đổi kiểu dữ liệu trong Java từ `String` thành `List<String>`.
   - Gắn annotation `@JdbcTypeCode(SqlTypes.JSON)`.
   - Loại bỏ hoàn toàn khối code parse JSON lồng kềnh dùng `ObjectMapper` trong `FraudDetectionServiceImpl`.

2. **ProductVariantEntity (`variant_attributes`)**
   - Đổi kiểu dữ liệu từ `String` thành `Map<String, String>`.
   - Gắn annotation `@JdbcTypeCode(SqlTypes.JSON)`.
   - **Tối ưu MapStruct**: Xóa bỏ các method parse thủ công `@AfterMapping` bên trong `ProductMapper.java` do MapStruct đã tự động hiểu và map từ Map (DTO) sang Map (Entity).
   - Cập nhật kiểu dữ liệu của thuộc tính này bên trong DTO `CartItemResponse` thành `Map<String, String>`.

### 2.4. Reseed dữ liệu User & Address
- Khởi tạo script Flyway mới **`V4.5__reseed_user_kiethuynh_and_address.sql`**.
- Xóa các dữ liệu cũ (để tránh conflict) và seed lại account `kiethuynh3499@gmail.com`.
- Mã hóa mật khẩu chuẩn Bcrypt (`crypt('Password@123', gen_salt('bf', 10))`) để đồng bộ với cơ chế xác thực của ứng dụng.
- Thêm sẵn một địa chỉ mặc định (TP. Hồ Chí Minh) cho user này để test API Checkout thuận tiện hơn mà không cần gọi API tạo address thủ công.

### 2.5. Xác nhận luồng Order Checkout & AI Decision Engine
- Đã rà soát lại `OrderServiceImpl.java`. Xác nhận luồng logic Checkout hoạt động chính xác theo sơ đồ:
  1. Đơn hàng được khởi tạo mặc định là `PENDING`.
  2. Gửi request sang AI Server lấy Decision. Nếu bị `DECLINE`, ném Exception Rollback toàn bộ (Hủy đơn hàng, trả lại Stock).
  3. Nếu được `APPROVE`, trạng thái được cập nhật tự động thành `AWAITING_SHIPMENT` (với COD) hoặc `PENDING_PAYMENT` (thanh toán Online).
  4. Nếu bị `REVIEW`, hệ thống đánh dấu đơn hàng thành `PENDING_REVIEW` để con người kiểm duyệt thủ công.

## 3. Các lưu ý (Next Steps)
- Backend Spring Boot cần được **Restart** để Hibernate apply bộ mapping JSONB mới từ hai Entities vừa sửa.
- Ở logic `FraudDetectionServiceImpl.java`, hệ thống hiện đang mock một số logic tạm thời (như `locationMismatch = 0`). Trong session tới, các features này sẽ được tích hợp việc phân tích location từ IP thực tế.
- Các API VNPAY/MoMo cho trạng thái `PENDING_PAYMENT` sẽ được implement trong tương lai.
