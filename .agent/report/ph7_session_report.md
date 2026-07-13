# Session Report: Triển khai Phân hệ 7 (AI Recommendation)

**Thời gian hoàn thành:** 13/07/2026  
**Module:** Phân hệ 7 - Trí tuệ nhân tạo (Gợi ý sản phẩm)  
**Trạng thái:** Hoàn tất (Implementation)

---

## 1. Mục tiêu phiên làm việc
Phiên làm việc tập trung vào việc thiết kế và xây dựng cơ sở hạ tầng ở tầng Backend (Database, Domain, Service, API) nhằm hỗ trợ hệ thống Gợi ý sản phẩm (AI Recommendation). Mục tiêu là tạo ra luồng dữ liệu (Data Pipeline) từ việc thu thập hành vi người dùng (Tracking) cho đến việc phục vụ kết quả của Mô hình Machine Learning (Serving) mà không làm ảnh hưởng đến hiệu suất hệ thống hiện tại.

## 2. Chi tiết hạng mục đã triển khai

### 2.1. Cấu trúc Database (Migrations)
Để chuẩn bị dữ liệu đầu vào và lưu trữ kết quả đầu ra cho các model (Offline processing):
- **Thu thập dữ liệu (Data Ingestion):** Khởi tạo migration `V6.4` mở rộng bảng `user_behaviors`. 
  - Bổ sung `session_id` (để theo dõi người dùng khách/vãng lai).
  - Đổi kiểu `variant_id` thành `BIGINT` nguyên thủy (không dùng Khóa ngoại - FK) để tránh lỗi cascade và exception không đáng có.
  - Bổ sung metadata dưới dạng `JSONB` để tăng tính linh hoạt khi thu thập tracking.
- **Phục vụ mô hình (Model Serving Tables):** Khởi tạo migration `V6.5` tạo 3 bảng Cache kết quả cho AI:
  - `product_similarities`: Lưu kết quả Content-Based / Item-Based CF (Các sản phẩm tương tự).
  - `user_recommendations`: Lưu kết quả Matrix Factorization (Gợi ý cá nhân hóa cho User).
  - `product_associations`: Lưu kết quả Apriori / FPGrowth (Các sản phẩm thường được mua cùng nhau).

### 2.2. Domain Layer & Repositories
- Cập nhật lại `UserBehaviorEntity` tương thích với cơ sở dữ liệu.
- Định nghĩa 3 Entities mới (`ProductSimilarityEntity`, `UserRecommendationEntity`, `ProductAssociationEntity`) với cấu trúc Composite Key (`@IdClass` / `@EmbeddedId` tùy tình huống) đảm bảo hiệu suất truy vấn.

### 2.3. Data Ingestion Pipeline (Tracking API)
- Xây dựng `TrackingController` với endpoint `/api/v1/tracking`.
- Xây dựng `TrackingServiceImpl` xử lý lưu trữ log.
- **Tối ưu hiệu năng:** Gắn `@Async` cho việc ghi nhận tracking để tách biệt ra khỏi Main Thread của request HTTP. Đảm bảo trải nghiệm mua sắm của khách hàng không bị chậm trễ (Zero impact on latency) bởi tính năng tracking.

### 2.4. Model Serving Pipeline (Recommendation API)
- Xây dựng `RecommendationController` với các endpoints:
  - `/api/v1/recommendations/personalized`
  - `/api/v1/recommendations/similar/{productId}`
  - `/api/v1/recommendations/bought-together/{productId}`
- Xây dựng `RecommendationServiceImpl` cho việc Query dữ liệu tốc độ cao.
- **Cơ chế Cold-Start (Dự phòng):** Nếu mô hình chưa kịp train cho User mới, hoặc khách hàng chưa đăng nhập, hệ thống sẽ tự động chuyển hướng sang danh sách **Sản phẩm bán chạy nhất còn hàng** (`findTop10ByInStockTrueOrderBySoldCountDesc`).

### 2.5. Data Retention (Tối ưu lưu trữ)
- Chuyển hướng từ sử dụng cơ chế Table Partitioning (PostgreSQL) sang dùng Scheduled Batch Job cho đơn giản và hiệu quả trong ngữ cảnh dự án.
- Tạo `UserBehaviorRetentionJob` (Spring `@Scheduled`) chạy vào lúc **3:00 AM sáng Chủ Nhật** hàng tuần. Tự động xóa các log lịch sử hành vi cũ hơn 6 tháng để giải phóng dung lượng DB.

### 2.6. Bug Fixes Phát sinh
- Phát hiện và bổ sung Constraint `UNIQUE (user_id, product_id)` bị thiếu ở Database cho module Wishlist (Đã khắc phục thông qua migration `V6.6`).
- Xử lý các lỗi ánh xạ Entity/Repository trong quá trình Context Load của Spring Boot (`getIsActive` đổi thành `getInStock`).

## 3. Kết quả Verification
- Toàn bộ source code biên dịch thành công (`BUILD SUCCESS`).
- Flyway tự động cập nhật Migration scripts từ V6.4, V6.5, V6.6 lên cơ sở dữ liệu hoàn hảo.
- Mối liên kết Object-Relational Mapping (ORM) thông qua Hibernate đã load thành công toàn bộ Context.

## 4. Pending / Next Steps
- **Phân hệ 8 (Risk & Support):** Tạm thời pending phần `risk_alerts` & `risk_rule_configs` theo yêu cầu. Module `contact_messages` đã hoàn thành.
- **Model Training:** Hiện tại hệ thống Spring Boot đã có khả năng lưu Data và trả về kết quả. Việc train AI model thực tế (Python/Jupyter Notebook) sẽ lấy data từ `user_behaviors` và đẩy kết quả vào 3 bảng AI thông qua Job bên ngoài trong tương lai.
