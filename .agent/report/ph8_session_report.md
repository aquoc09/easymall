# Session Report: Phân hệ 8 - Implement Risk Engine & Loại bỏ Fraud Logic

**Thời gian hoàn thành:** Bắt đầu từ 2026-07-18
**Mục tiêu chính:** Chuyển đổi từ mô hình Fraud Detection (đồng bộ) sang Risk Rule Engine (bất đồng bộ) thông qua luồng Event-Driven để đảm bảo hiệu năng và tính linh hoạt.

## 1. Phân tích & Đánh giá (Analysis)
Dựa trên tài liệu `new_logic.md`, logic phân tích gian lận (Fraud) cũ với `FraudDetectionService` hoạt động theo mô hình Synchronous làm tăng độ trễ khi checkout. Hướng đi mới (Risk Engine) sẽ:
- Hoạt động bất đồng bộ (Asynchronous) dựa trên Spring Application Events.
- Cấu hình linh hoạt qua Database (Admin có thể tự điều chỉnh ngưỡng giới hạn).
- Đưa ra Risk Alert (Cảnh báo) thay vì Throw Exception ngay lúc tạo đơn.

## 2. Các thay đổi chính

### 2.1 Database Schema & Flyway
- **Tạo script `V6.6__refactor_fraud_to_risk.sql`:** 
  - `DROP TABLE` các bảng cũ: `fraud_detections`, `fraud_rules`.
  - `CREATE TABLE` mới: `risk_rule_configs` (quản lý bộ luật), `risk_alerts` (lưu trữ cảnh báo).
- **Tạo script `V6.7__seed_risk_permissions.sql`:**
  - Seed các quyền mới: `risk_rule:manage`, `risk_alert:view`, `risk_alert:resolve`.

### 2.2 Refactor CodeBase
- **Gỡ bỏ code cũ:** Xóa toàn bộ Entity, Repository, Service của mô hình Fraud cũ. 
- **Sửa đổi `OrderServiceImpl`:** 
  - Không gọi `FraudDetectionService` nữa.
  - Sử dụng `ApplicationEventPublisher` để phát ra `OrderCreatedEvent` ngay sau khi lưu Order thành công.

### 2.3 Triển khai Risk Engine Mới
- **Tạo mới `RiskRuleEngine` (@Service):**
  - Lắng nghe event bằng `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)`.
  - Kết hợp annotation `@Async` và `@Transactional(propagation = Propagation.REQUIRES_NEW)` để engine hoạt động hoàn toàn độc lập với flow mua hàng.
  - Xây dựng logic quét qua danh sách `activeRules` lấy từ `risk_rule_configs`.
- **Logic Đánh giá Luật (Rules):**
  - `R1_MULTIPLE_DEVICES`: Kiểm tra số lượng thiết bị đăng nhập khác nhau của User (`countDistinctDevicesByUserSince`).
  - `R2_FAILED_PAYMENTS`: Kiểm tra lượng đơn hàng bị `CANCELLED` trong khoảng thời gian cấu hình (`countOrdersByUserAndStatusSince`).
  - `R5_NEW_ACC_HIGH_VALUE`: Kiểm tra người dùng mới tạo (dưới 7 ngày) nhưng đặt đơn giá trị vượt ngưỡng (`finalPaymentMoney`).
- Nếu vi phạm, hệ thống tự động ghi lại một Record vào bảng `risk_alerts`.

### 2.4 Quản trị Risk Engine (Admin APIs)
- **Cung cấp `RiskAdminController` và `RiskService`:**
  - `GET /rules`: Xem danh sách tất cả các rules.
  - `PUT /rules/{ruleCode}`: Admin thay đổi cấu hình, ngưỡng quét, khung thời gian.
  - `GET /alerts`: Lấy danh sách cảnh báo, lọc theo `PENDING/RESOLVED`.
  - `POST /alerts/{alertId}/resolve`: Admin xác nhận đánh giá rủi ro (lưu lại ghi chú & thao tác xử lý).

## 3. Quá trình gỡ lỗi (Debugging) trong phiên làm việc
Trong phiên làm việc, chúng ta đã phát hiện và xử lý ngay 2 lỗi trước khi ứng dụng khởi chạy thành công:
1. **Lỗi Runtime truy vấn (JPQL):** Entity `OrderEntity` sử dụng thuộc tính `orderDate` nhưng trong Repository (`OrderRepository`) lúc đầu gọi nhầm thuộc tính `createdAt`. Lỗi đã được phát hiện qua log lúc khởi động ứng dụng và sửa lại truy vấn:
   - Sửa: `WHERE o.createdAt >= :since` ➔ `WHERE o.orderDate >= :since`.
2. **Lỗi TransactionalEventListener:** Spring Boot báo lỗi không thể dùng `@TransactionalEventListener` chung với `@Transactional` thông thường do rủi ro tranh chấp transaction context.
   - Khắc phục: Sửa `@Transactional` thành `@Transactional(propagation = Propagation.REQUIRES_NEW)` để cô lập transaction của Risk Rule.

## 4. Kết quả & Đóng góp cho Hệ thống
- **Hiệu năng:** Khách hàng không còn bị block chờ logic quét gian lận lúc bấm thanh toán (Checkout diễn ra tức thời).
- **Linh hoạt:** Admin có giao diện và API để quản lý cảnh báo cũng như tinh chỉnh luật linh hoạt (đổi ngưỡng 1 triệu thành 5 triệu) mà không cần code lại.
- Phân hệ 8 (Fraud -> Risk Engine) đã hoàn thành xuất sắc nhiệm vụ đặt ra.
