## **PHẦN 1: PHÂN HỆ HÒM THƯ GÓP Ý (CONTACT MESSAGES MODULE)**

_Chiến lược thiết kế: Khối lượng tinh giản (One-shot Feedback) nhằm tối ưu thời gian triển khai, phục vụ luồng giao tiếp một chiều nhưng vẫn đảm bảo tính toàn vẹn dữ liệu cho khách vãng lai và bảo toàn lịch sử hệ thống._

### **1.1. Mục đích và Vai trò**

- **Vai trò:** Là kênh tiếp nhận thông tin duy nhất từ người dùng cuối (phản ánh, khiếu nại, góp ý, báo lỗi) gửi đến Ban quản trị EasyMall.
- **Mục đích:** Tập trung hóa dữ liệu thắc mắc thay vì để phân tán qua hệ thống email rời rạc, hỗ trợ Admin có một Dashboard quản lý tập trung và dễ dàng đối soát.
- **Actor:** Customer (Khách hàng đã đăng nhập), Guest (Khách vãng lai chưa có tài khoản), Admin/Staff (Người quản trị).

### **1.2. Phân tích Trường dữ liệu (Data Dictionary)**

| Trường (Column) | Kiểu dữ liệu | Ý nghĩa & Mục đích                | Ràng buộc / Validate tại Backend                                                                                         | Actor cập nhật |
| :-------------- | :----------- | :-------------------------------- | :----------------------------------------------------------------------------------------------------------------------- | :------------- |
| message_id      | BIGINT       | Khóa chính, định danh duy nhất.   | Tự tăng (IDENTITY).                                                                                                      | System         |
| user_id         | BIGINT       | ID khách hàng (nếu đã đăng nhập). | Khóa ngoại. NULL (Cho phép Guest gửi). Dùng ON DELETE SET NULL để giữ lại lịch sử tin nhắn khi khách hàng xóa tài khoản. | Customer       |
| guest_name      | VARCHAR(100) | Tên của khách vãng lai.           | **Bắt buộc (NOT NULL ở tầng Logic)** nếu user_id là NULL.                                                                | Guest          |
| guest_email     | VARCHAR(100) | Email để Admin liên hệ phản hồi.  | **Bắt buộc (NOT NULL ở tầng Logic)** nếu user_id là NULL. Đúng định dạng Email.                                          | Guest          |
| subject         | VARCHAR(200) | Tiêu đề tóm tắt vấn đề.           | NOT NULL, Min 10 ký tự, Max 200 ký tự.                                                                                   | Customer/Guest |
| content         | TEXT         | Nội dung chi tiết góp ý/báo lỗi.  | NOT NULL, Min 20 ký tự. Lọc sạch mã độc (XSS Prevention).                                                                | Customer/Guest |
| status          | VARCHAR(20)  | Trạng thái xử lý của Admin.       | Default: PENDING. Được bảo vệ bởi CHECK (status IN ('PENDING', 'RESOLVED', 'REJECTED')).                                 | Admin          |
| created_at      | TIMESTAMPTZ  | Thời điểm gửi yêu cầu.            | DEFAULT CURRENT_TIMESTAMP. Không cho phép sửa.                                                                           | System         |

### **1.3. Đặc tả Use Case & Luồng xử lý (Data Flow)**

**Use Case 1: Gửi hòm thư góp ý (Customer / Guest)**

- **Business Rules (Quy tắc nghiệp vụ):**
  1. _Xác thực danh tính mềm:_ Nếu request được gửi có đính kèm JWT Token (Customer), Backend tự động trích xuất user_id và bỏ qua kiểm tra guest_name, guest_email. Nếu không có Token (Guest), Backend bắt buộc phải validate có guest_name và guest_email trong payload gửi lên.
  2. _Chống Spam (Rate Limiting):_ Một địa chỉ IP hoặc một user_id chỉ được gửi tối đa 3 thư liên hệ trong vòng 1 giờ để tránh tấn công rác (Spam).
- **Luồng xử lý (Data Flow):**
  1. **Frontend (React):** Render Form liên hệ. Nếu khách chưa login, hiện thêm ô nhập Tên và Email.
  2. **Khách hàng:** Nhập liệu -> Bấm Submit (Gửi Axios POST /api/v1/contacts).
  3. **Backend (Spring Boot):** Tiếp nhận Request -> Validate DTO (độ dài, email hợp lệ, kiểm tra chéo Guest/User) -> Chặn XSS.
  4. **Database:** Lưu thông tin vào bảng contact_messages.
  5. **Response:** Trả về HTTP 201 Created kèm thông báo _"Gửi yêu cầu thành công, chúng tôi sẽ phản hồi qua email"_.

**Use Case 2: Quản lý & Xử lý thư (Admin)**

- **Business Rules (Quy tắc nghiệp vụ):**
  1. Admin không được phép sửa hay xóa subject, content, hay email của khách hàng (Nguyên tắc bảo toàn chứng cứ).
  2. Admin chỉ có quyền đọc nội dung và chuyển đổi status sang RESOLVED (Đã xử lý thỏa đáng) hoặc REJECTED (Thư rác/Yêu cầu không hợp lệ).
- **Luồng xử lý (Data Flow):**
  1. **Admin Dashboard:** Truy cập trang "Quản lý Góp ý". Frontend gọi API GET /api/v1/admin/contacts?status=PENDING&page=0.
  2. **Database:** PostgreSQL thực thi Query siêu tốc O(1) nhờ tận dụng **Index chiến lược** idx_contact_dashboard, lập tức bốc ra các thư chưa xử lý và mới nhất đẩy lên đầu.
  3. **Admin:** Đọc nội dung thư, thực hiện việc gửi email phản hồi thủ công qua Gmail/Hệ thống mail nội bộ (do kiến trúc tối giản).
  4. **Cập nhật hệ thống:** Sau khi gửi mail xong, Admin bấm "Đánh dấu đã xử lý". Frontend gọi PATCH API. Backend cập nhật status = 'RESOLVED'. Hồ sơ khép lại.
