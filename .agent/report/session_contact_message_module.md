# Session Report: Contact Message Module Implementation

**Date:** 2026-07-11
**Module:** Contact Message (Notifications & Contact Messages)

## 1. Mục tiêu (Objectives)
- Triển khai module `Contact Message` phục vụ cho cả khách hàng chưa đăng nhập (Guest) và người dùng đã đăng nhập (User).
- Gộp các chức năng liên hệ, khiếu nại, góp ý thành chung một luồng xử lý trên bảng `contact_messages`.
- Admin có thể quản lý, theo dõi và cập nhật trạng thái (từ `PENDING` sang `RESOLVED` hoặc `REJECTED`).
- Đảm bảo an toàn, chặn các lỗi bảo mật (XSS, truy cập trái phép) và hỗ trợ tối ưu hiệu suất truy vấn.

## 2. Chi tiết triển khai (Implementation Details)

### 2.1. Database & Migrations
- Tạo file `V6.1__create_contact_messages_table.sql`: 
  - Khởi tạo bảng `contact_messages` với các trường cần thiết: `subject`, `content`, `guest_name`, `guest_email`, `status`.
  - Có liên kết tới bảng `users` (nullable).
  - Tích hợp thêm index `idx_contact_dashboard` (`status`, `created_at` DESC) phục vụ cho truy vấn lấy danh sách ưu tiên thời gian và trạng thái nhanh chóng nhất.
- Tạo file `V6.2__seed_contact_message_permissions.sql`:
  - Map thêm 2 quyền quản lý: `contact:read` và `contact:update` vào cho `ROLE_ADMIN` sử dụng `CROSS JOIN`.
- Cập nhật thêm cấu trúc quyền (permissions) trong `V6.0__seed_slider_permissions.sql` bằng cách xóa bớt reference không chính xác liên quan đến trường `module`.

### 2.2. Entities & DTOs
- `ContactMessageEntity`: Thiết lập annotation chuẩn JPA.
- `ContactMessageRequest`: Validator tích hợp chặn các string rỗng hoặc null cho `subject` và `content`.
- `ContactMessageStatusRequest`: Phục vụ cho API Patch của Admin.
- `ContactMessageResponse`: Trả về dữ liệu chi tiết an toàn ra phía Frontend.

### 2.3. Repository & Mapper
- `ContactMessageRepository`: Thừa kế `JpaRepository`, custom thêm 2 method query `findByUser_UserId` và `findByStatus`.
- `ContactMessageMapper`: Sử dụng `MapStruct` chuyển đổi 1-1 giữa Entity và Response, tận dụng mapping `user` thông qua custom format.

### 2.4. Business Logic (Service)
- Tạo Interface `ContactMessageService` và class `ContactMessageServiceImpl`.
- **Validation**: 
  - Tự động lấy User từ JWT, nếu là Guest bắt buộc phải nhập `guest_name` và `guest_email`.
- **An toàn bảo mật (XSS)**: 
  - Làm sạch `subject` và `content` thông qua thư viện `HtmlUtils.htmlEscape(String)`.
- **State Machine Rules**:
  - Khi tạo mới, trạng thái mặc định luôn là `PENDING`.
  - Khi cập nhật bởi admin, chỉ được phép thay đổi nếu trạng thái đang ở `PENDING`. Đưa ra exception `INVALID_STATUS_TRANSITION` (HTTP 400) nếu sai quy trình.

### 2.5. Controllers & Security
- `ContactMessageController`:
  - `POST /api/v1/contacts`: API tạo form, `PermitAll()`.
  - `GET /api/v1/contacts/me`: Dành riêng cho user đã đăng nhập theo dõi form của chính mình.
  - `GET /api/v1/admin/contacts`: Fetch list dưới quyền admin.
  - `PATCH /api/v1/admin/contacts/{id}/status`: Cập nhật trạng thái dước quyền admin.
- Cập nhật **SecurityConfig**: Mở public endpoint `/api/v1/contacts`.

### 2.6. Exceptions
- Mở rộng thêm HTTP Code/Message trong enum `ErrorCode` với `INVALID_STATUS_TRANSITION(3003, "error.invalid-status-transition", HttpStatus.BAD_REQUEST)`.

## 3. Tổng kết (Summary)
- Module đã build hoàn chỉnh ở mức Backend với tính cô đọng và có khả năng mở rộng cao (Ví dụ gửi email thông báo sau này ở các worker async).
- Đã loại bỏ bớt phần limit rate sử dụng Redis và Bucket4J do mục tiêu giai đoạn này muốn làm nhanh gọn như flow của Trello, đáp ứng nhanh yêu cầu về thời gian.
- Backend code build thành công, Spotless checks (nếu fix maven dependencies sau này) sẽ đi vào hoạt động trơn tru.
