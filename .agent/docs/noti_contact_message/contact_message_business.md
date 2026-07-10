# TÀI LIỆU ĐẶC TẢ NGHIỆP VỤ (BRD & SRS)

**Phân hệ:** Quản lý Liên hệ - Góp ý - Khiếu nại (Contact Messages Module)
**Phiên bản:** 1.0 (Bản đệ trình Hội đồng)

## 1. TỔNG QUAN MODULE

- **Mục tiêu:** Cung cấp kênh giao tiếp một chiều (Inbound) chính thức để tiếp nhận thắc mắc, phản hồi, khiếu nại từ người dùng cuối đến Ban quản trị (Admin).
- **Vai trò trong hệ thống:** Đóng vai trò như một hệ thống Helpdesk cơ bản. Giúp tập trung hóa dữ liệu chăm sóc khách hàng, thay thế cho việc nhận email rời rạc, hỗ trợ phân luồng và quản lý SLA (Service Level Agreement) nội bộ.
- **Các Actor tham gia:**
  - Guest: Khách vãng lai chưa có tài khoản.
  - Customer: Khách hàng đã đăng nhập.
  - Admin: Nhân viên chăm sóc khách hàng / Quản trị viên.
- **Luồng hoạt động tổng quát:** Actor (Guest/Customer) gửi Form liên hệ → Hệ thống lưu trữ trạng thái PENDING → Admin tiếp nhận trên Dashboard → Admin xử lý (gửi email phản hồi thủ công) → Đóng Ticket với trạng thái RESOLVED hoặc REJECTED.

## 2. ĐẶC TẢ CHI TIẾT BẢNG contact_messages

| Trường      | Ý nghĩa & Mục đích                                      | Sinh / Cập nhật / Không được cập nhật                          | Ràng buộc / Validation                                              | Ví dụ Hợp lệ / Không Hợp lệ            |
| :---------- | :------------------------------------------------------ | :------------------------------------------------------------- | :------------------------------------------------------------------ | :------------------------------------- |
| message_id  | Khóa chính, định danh duy nhất của thư.                 | **Sinh:** Lúc Insert. **Cập nhật:** KHÔNG.                     | Tự tăng (IDENTITY).                                                 | Hợp lệ: 105                            |
| user_id     | Định danh khách hàng nếu đã Login. Để liên kết lịch sử. | **Sinh:** Từ JWT Token. **Cập nhật:** Khi xóa user (SET NULL). | NULL (Dành cho Guest). Nếu có, phải tồn tại trong bảng users.       | Hợp lệ: 55                             |
| guest_name  | Tên của khách vãng lai để xưng hô.                      | **Sinh:** Lúc Insert. **Cập nhật:** KHÔNG.                     | NOT NULL (Tầng logic) nếu user_id là NULL. Độ dài < 100.            | Hợp lệ: Nguyễn Văn A                   |
| guest_email | Email để Admin gửi thư phản hồi lại.                    | **Sinh:** Lúc Insert. **Cập nhật:** KHÔNG.                     | NOT NULL (Tầng logic) nếu user_id là NULL. Chuẩn định dạng Email.   | Không hợp lệ: abc@.com                 |
| subject     | Tiêu đề tóm tắt (Phân loại vấn đề).                     | **Sinh:** Lúc Insert. **Cập nhật:** KHÔNG.                     | NOT NULL. Giới hạn 200 ký tự. Cố định danh sách (Dropdown trên UI). | Hợp lệ: [Khiếu nại] Đơn hàng chưa giao |
| content     | Nội dung chi tiết lời nhắn.                             | **Sinh:** Lúc Insert. **Cập nhật:** KHÔNG.                     | NOT NULL. Tránh HTML injection. Min 10 ký tự.                       | Không hợp lệ: a                        |
| status      | Trạng thái xử lý của Ban quản trị.                      | **Sinh:** Mặc định PENDING. **Cập nhật:** Bởi Admin.           | Chỉ nhận 3 giá trị cố định.                                         | Hợp lệ: RESOLVED                       |
| created_at  | Thời điểm ghi nhận hệ thống.                            | **Sinh:** Database tự lấy giờ Server. **Cập nhật:** KHÔNG.     | Không cho phép client can thiệp.                                    | Hợp lệ: 2026-07-10 10:00:00+07         |

- **Giải thích CONSTRAINT chk_contact_status:** Bảo vệ tính toàn vẹn dữ liệu ở tầng vật lý (Database Level). Đảm bảo không có dòng code Backend nào bị lỗi có thể Insert các trạng thái rác như "PROCESSING", "DONE" vào DB.
- **Giải thích INDEX idx_contact_dashboard:** Composite Index này được tạo dựa trên hành vi truy vấn lõi của Admin: _Lấy các thư chưa đọc (status = 'PENDING') và mới nhất xếp lên đầu (created_at DESC)_. Tránh Full-Table Scan khi dữ liệu lớn.

## 3. LUỒNG NGHIỆP VỤ CHI TIẾT (WORKFLOW)

**3.1. Luồng Gửi Liên hệ (Guest / Customer)**

1. **Frontend:** Render Form. Nếu chưa Login, hiển thị thêm ô Tên và Email. Cột subject sử dụng thẻ <select> (Dropdown) với các danh mục có sẵn.
2. **Backend (Spring Boot):** Kiểm tra SecurityContext.
   - Nếu có JWT → Lấy user_id, bỏ qua validate guest_name/email.
   - Nếu Anonymous → Bắt buộc validate guest_name và guest_email.
3. **Database:** Lưu bản ghi với status = 'PENDING'.
4. **Response:** Trả về mã HTTP 201 Created.

**3.2. Luồng Xử lý (Admin Dashboard)**

1. **Frontend:** Gọi API lấy danh sách tin nhắn.
2. **Backend:** SELECT \* FROM contact_messages WHERE status = ? ORDER BY created_at DESC LIMIT 20 OFFSET 0. (Ăn thẳng vào Index).
3. **Admin:** Xem chi tiết nội dung. Thực hiện hỗ trợ khách hàng (Gửi email giải quyết bằng hệ thống mail doanh nghiệp, hoặc gọi điện).
4. **Admin:** Bấm nút "Đánh dấu Đã xử lý" / "Từ chối".
5. **Database:** UPDATE contact_messages SET status = 'RESOLVED' WHERE message_id = ?.

## 4. QUY TẮC NGHIỆP VỤ (BUSINESS RULES)

1. **Tính Bất biến (Immutability):** Dữ liệu liên hệ là Bằng chứng giao tiếp. Người dùng KHÔNG ĐƯỢC phép sửa hoặc xóa lời nhắn sau khi đã nhấn gửi. Admin KHÔNG ĐƯỢC phép sửa nội dung (subject, content) của khách.
2. **Rule định danh:** Guest bắt buộc nhập Tên và Email. Customer (đã login) không cần nhập, Backend tự link tới bảng users qua user_id.
3. **Luật bảo toàn lịch sử:** Khi tài khoản khách hàng bị xóa khỏi bảng users, lịch sử liên hệ trong bảng contact_messages vẫn được giữ nguyên để phục vụ đối soát, cột user_id tự động chuyển thành NULL.
4. **State Machine (Chuyển trạng thái):** Trạng thái chỉ được đi từ PENDING → RESOLVED hoặc REJECTED. Không có chiều ngược lại.

## 5. VALIDATION VÀ DATA SANITIZATION

**Frontend (React UI):**

- guest_email: Regex chuẩn RFC 5322.
- subject: Bắt buộc chọn từ Dropdown List (Không cho gõ tự do).
- content: Trim() khoảng trắng hai đầu. Báo lỗi đỏ nếu < 10 ký tự.

**Backend (Spring Boot @Valid):**

- @NotBlank, @Size(max=200) cho Subject.
- @Email cho guest_email (Nếu Anonymous).
- **Chống XSS/HTML Injection:** Dùng thư viện HtmlUtils.htmlEscape(content) hoặc thư viện OWASP AntiSamy trước khi Insert vào DB để lọc thẻ <script>, <iframe> độc hại.

## 6. CHỐNG SPAM VÀ BẢO MẬT (SECURITY STRATEGY)

| Kịch bản Tấn công                 | Hậu quả                                            | Giải pháp tại Spring Boot & DB                                                                                                            |
| :-------------------------------- | :------------------------------------------------- | :---------------------------------------------------------------------------------------------------------------------------------------- |
| **Spam gửi liên tục (Bot/Flood)** | Làm đầy Database, làm tắc nghẽn luồng xử lý Admin. | **Rate Limiting:** Sử dụng thư viện Bucket4j kết hợp Redis. Cấu hình: 1 địa chỉ IP / 1 user_id chỉ được gửi tối đa **3 request / 1 giờ**. |
| **Duplicate Submit (Bấm 2 lần)**  | Sinh ra 2 bản ghi giống hệt nhau cùng lúc.         | **Frontend:** Disable nút Submit ngay khi click. **Backend:** Bỏ qua request nếu có cùng subject + content + ip trong vòng 10 giây.       |
| **SQL Injection**                 | Lộ lọt, xóa dữ liệu toàn bộ DB.                    | Sử dụng **Spring Data JPA / Hibernate**. Tự động áp dụng Parameterized Queries (PreparedStatement). Triệt tiêu 100% SQLi.                 |
| **Gửi Email giả mạo**             | Admin gửi thư trả lời nhưng bị bounce (bật lại).   | Sử dụng Regex chặt chẽ ở DTO. Đối với hệ thống thật sẽ tích hợp email verify, nhưng ở phạm vi luận văn: Chấp nhận rủi ro này.             |

## 7. XỬ LÝ LỖI WORKFLOW (ERROR HANDLING MATRIX)

| Lỗi phát sinh                                         | Mã HTTP               | Thông báo cho Frontend                                       | Hành động của Hệ thống                                                                                       |
| :---------------------------------------------------- | :-------------------- | :----------------------------------------------------------- | :----------------------------------------------------------------------------------------------------------- |
| Guest không truyền Email                              | 400 Bad Request       | _"Vui lòng nhập Email để chúng tôi phản hồi."_               | Bắt lỗi tại @RestControllerAdvice, ném lỗi Validation.                                                       |
| Vượt quá giới hạn (Rate Limit)                        | 429 Too Many Requests | _"Bạn đang thao tác quá nhanh, vui lòng thử lại sau 1 giờ."_ | Chặn ở tầng Filter/Interceptor, không chạm tới Database.                                                     |
| Cập nhật trạng thái đồng thời (Admin A và B cùng bấm) | 200 OK                | (Thành công bình thường)                                     | PostgreSQL xử lý Update cuối cùng thắng (Last Write Wins). Không gây lỗi nghiệp vụ do trạng thái giống nhau. |

## 8. THIẾT KẾ REST API CONTRACTS

### 8.1. Khách hàng gửi liên hệ (Dùng chung cho Guest & Customer)

- **Endpoint:** POST /api/v1/contacts
- **Auth:** Không bắt buộc (PermitAll).
- **Request Body:** {"subject": "Lỗi thanh toán", "content": "Tài khoản bị trừ...", "guestName": "Khôi", "guestEmail": "khoi@gmail.com"}
- **Logic BE:** Check JWT Header. Nếu có JWT → lấy user_id, set Name/Email = null. Nếu không JWT → Yêu cầu Name/Email.

### 8.2. Admin lấy danh sách (Admin Dashboard)

- **Endpoint:** GET /api/v1/admin/contacts?status=PENDING&page=0&size=20
- **Auth:** Bắt buộc Role ADMIN.
- **Response:** Trả về đối tượng Page<ContactResponseDTO>.

### 8.3. Admin Đổi trạng thái xử lý

- **Endpoint:** PATCH /api/v1/admin/contacts/{messageId}/status
- **Auth:** Role ADMIN.
- **Request Body:** {"status": "RESOLVED"}
- **Logic BE:** Verify messageId tồn tại. Kiểm tra DTO Status nằm trong tập enum hợp lệ. Gọi Repository.updateStatus().

## 9. MA TRẬN PHÂN QUYỀN (AUTHORIZATION)

- GUEST: Lọc ở lớp Filter. Chỉ được gọi POST /api/v1/contacts.
- CUSTOMER: Lọc qua JWT. Được gọi POST và GET /api/v1/contacts/me (Lịch sử của mình).
- ADMIN: Lọc qua Role hasRole('ADMIN'). Toàn quyền xem, lọc và đổi trạng thái trên các API có prefix /admin/\*.

### XÁC ĐỊNH ACTOR (TÁC NHÂN)

Để hệ thống tinh gọn, chúng ta chỉ cần 3 Actor chính:

1. **Guest (Khách vãng lai):** Người dùng chưa đăng nhập. Cần nhập Tên và Email để hệ thống nhận diện.
2. **Customer (Khách hàng):** Người dùng đã đăng nhập hệ thống.
3. **Admin (Quản trị viên / CSKH):** Người tiếp nhận, quản lý và thay đổi trạng thái của yêu cầu.
   _(Lưu ý: Không cần thêm Actor System hay Email Service vào sơ đồ Use Case để tránh làm phức tạp biểu đồ, những yếu tố đó sẽ được mô tả trong Sequence Diagram hoặc Luồng xử lý)._

### 3. THIẾT KẾ USE CASE CHI TIẾT (TỐI ƯU CHO 1 TUẦN)

**Nguyên tắc tối ưu:** Không tách "Gửi góp ý", "Gửi khiếu nại" thành các Use Case riêng biệt. Bản chất chúng chỉ là dữ liệu chọn từ Dropdown subject. Hãy gom chúng lại thành 1 Use Case duy nhất: **"Gửi yêu cầu hỗ trợ"**.

**Danh sách Use Case rút gọn:**

- **Guest:**
  - UC01: Gửi yêu cầu hỗ trợ.
- **Customer:**
  - UC01: Gửi yêu cầu hỗ trợ.
  - UC02: Xem lịch sử yêu cầu (Của cá nhân).
- **Admin:**
  - UC03: Xem danh sách yêu cầu hỗ trợ (Bao gồm Lọc/Tìm kiếm).
  - UC04: Xem chi tiết yêu cầu.
  - UC05: Cập nhật trạng thái yêu cầu (Đóng/Từ chối).

### 4. MÔ TẢ CHI TIẾT USE CASE (USE CASE SPECIFICATION)

Dưới đây là mẫu đặc tả chuẩn cho 2 Use Case quan trọng nhất để bạn đưa vào luận văn.

#### UC01: Gửi yêu cầu hỗ trợ (Submit Support Request)

- **Mục đích:** Cho phép người dùng (Guest/Customer) gửi thắc mắc, báo lỗi hoặc khiếu nại đến ban quản trị.
- **Actor:** Guest, Customer.
- **Điều kiện tiên quyết:** Không có.
- **Luồng xử lý chính (Basic Flow):**
  - Người dùng truy cập trang "Hỗ trợ / Liên hệ".
  - Hệ thống hiển thị Form. (Nếu là Customer, tự động ẩn ô Tên và Email).
  - Người dùng chọn Chủ đề (subject dropdown), nhập Nội dung (content).
  - Người dùng nhấn "Gửi".
  - Hệ thống kiểm tra Validation và Rule chống Spam (Rate Limit).
  - Hệ thống lưu vào Database với trạng thái PENDING.
  - Hệ thống thông báo gửi thành công.
- **Luồng xử lý ngoại lệ (Alternate Flow):**
  - _3a. Bỏ trống thông tin:_ Hệ thống báo lỗi đỏ tại các trường tương ứng, yêu cầu nhập lại.
  - _5a. Vi phạm Rate Limit (Spam):_ Hệ thống báo lỗi "Bạn thao tác quá nhanh, thử lại sau ít phút". Không lưu DB.
- **Điều kiện kết thúc:** Bản ghi mới xuất hiện trong bảng contact_messages.
- **Dữ liệu tác động:** INSERT INTO contact_messages.

#### UC05: Cập nhật trạng thái yêu cầu (Update Request Status)

- **Mục đích:** Admin đánh dấu yêu cầu đã được xử lý xong hoặc từ chối yêu cầu rác.
- **Actor:** Admin.
- **Điều kiện tiên quyết:** Admin đã đăng nhập và đang ở trang Chi tiết yêu cầu.
- **Luồng xử lý chính:**
  1. Admin nhấn nút "Đánh dấu Đã xử lý" (hoặc "Từ chối").
  2. Hệ thống hiển thị Popup xác nhận.
  3. Admin nhấn "Đồng ý".
  4. Hệ thống cập nhật trạng thái status thành RESOLVED (hoặc REJECTED).
  5. Hệ thống hiển thị thông báo thành công và tải lại danh sách.
- **Điều kiện kết thúc:** Cột status trong DB được thay đổi.
- **Dữ liệu tác động:** UPDATE contact_messages SET status = ... WHERE message_id = ?.

### 5. HƯỚNG DẪN VẼ SƠ ĐỒ USE CASE UML

Khi bạn dùng StarUML hoặc Draw.io, hãy cấu trúc sơ đồ như sau:

- **Boundary (Khung hệ thống):** Vẽ một hình chữ nhật lớn, đặt tên là "Phân hệ Quản lý Yêu cầu hỗ trợ".
- **Bố trí Actor:** Đặt Guest và Customer ở bên trái. Đặt Admin ở bên phải.
- **Kết nối (Associations):**
  - Kéo mũi tên từ Guest và Customer vào hình oval [Gửi yêu cầu hỗ trợ].
  - Kéo mũi tên từ Customer vào [Xem lịch sử yêu cầu].
  - Kéo mũi tên từ Admin vào [Quản lý danh sách yêu cầu] và [Cập nhật trạng thái yêu cầu].
- **Mối quan hệ <<include>> (Bắt buộc):**
  - Từ [Xem lịch sử yêu cầu] → <<include>> → [Đăng nhập].
  - Từ [Cập nhật trạng thái yêu cầu] → <<include>> → [Đăng nhập].
- **Mối quan hệ <<extend>> (Điểm A+ của sơ đồ):**
  - Vẽ một Use Case phụ là [Nhập thông tin Tên và Email].
  - Kéo mũi tên nét đứt từ [Nhập thông tin Tên và Email] chỉ ngược về [Gửi yêu cầu hỗ trợ] với nhãn <<extend>>.
  - _Giải thích trước hội đồng:_ "Dạ thưa thầy cô, hành động nhập Tên và Email chỉ xảy ra mở rộng (extend) khi Actor là Guest. Nếu là Customer đã đăng nhập thì luồng extend này không được kích hoạt ạ".

### 6. KIỂM TRA TÍNH ĐẦY ĐỦ VÀ LOẠI BỎ CHỨC NĂNG THỪA

- **Có thiếu không?** Không. Đã bao phủ 100% các cột trong bảng contact_messages.
- **Có trùng lặp không?** Không. Đã gom "Góp ý/Khiếu nại/Liên hệ" thành 1 UC duy nhất.
- **CHỨC NĂNG CẦN LOẠI BỎ (ĐỂ CỨU DEADLINE 1 TUẦN):**
  - ❌ _Chức năng "Gửi email tự động xác nhận cho khách":_ Bỏ! Chỉ cần hiển thị Popup "Thành công" trên UI React là đủ. Tích hợp SMTP mail lúc này rất tốn thời gian test.
  - ❌ _Chức năng "Đính kèm hình ảnh":_ Bỏ! Nó đòi hỏi cấu hình Cloudinary/S3, bảng CSDL phải thêm cột. Không đáng để mạo hiểm trong tuần cuối.

### 7. BẢNG TỔNG HỢP USE CASE (ĐỂ ĐƯA VÀO LUẬN VĂN)

Dưới đây là bảng chuẩn form học thuật để bạn dán vào file Word báo cáo:
| Module | Actor | Mã UC | Tên Use Case | Mô tả ngắn | Mức độ ưu tiên |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Quản lý Yêu cầu hỗ trợ** | Guest, Customer | UC_SUP_01 | Gửi yêu cầu hỗ trợ | Cho phép người dùng gửi lời nhắn, thắc mắc hoặc báo lỗi đến hệ thống. Phân loại theo Dropdown Subject. | **Bắt buộc** |
| | Customer | UC_SUP_02 | Xem lịch sử yêu cầu | Khách hàng đã login xem lại các lời nhắn mình đã gửi và tình trạng xử lý. | _Nên có_ |
| | Admin | UC_SUP_03 | Xem danh sách yêu cầu | Admin xem danh sách, lọc các yêu cầu đang PENDING và sắp xếp mới nhất lên đầu. | **Bắt buộc** |
| | Admin | UC_SUP_04 | Xem chi tiết yêu cầu | Đọc toàn bộ nội dung content và thông tin người gửi. | **Bắt buộc** |
| | Admin | UC_SUP_05 | Cập nhật trạng thái | Đổi trạng thái từ PENDING sang RESOLVED hoặc REJECTED sau khi đã xử lý ngoài hệ thống. | **Bắt buộc** |
Bạn có muốn tôi hỗ trợ thêm về việc soạn thảo nội dung hay cấu trúc lại các phần trong tài liệu này không?
Bỏ qua các thông tin vẽ biểu đồ
Câu trả lời của Gemini
Dưới đây là nội dung tài liệu của bạn đã được chuyển đổi sang định dạng Markdown, đã loại bỏ các hướng dẫn và thông tin liên quan đến việc vẽ biểu đồ.

# TÀI LIỆU ĐẶC TẢ NGHIỆP VỤ (BRD & SRS)

**Phân hệ:** Quản lý Liên hệ - Góp ý - Khiếu nại (Contact Messages Module)
**Phiên bản:** 1.0 (Bản đệ trình Hội đồng)

## 1. TỔNG QUAN MODULE

- **Mục tiêu:** Cung cấp kênh giao tiếp một chiều (Inbound) chính thức để tiếp nhận thắc mắc, phản hồi, khiếu nại từ người dùng cuối đến Ban quản trị (Admin).
- **Vai trò trong hệ thống:** Đóng vai trò như một hệ thống Helpdesk cơ bản. Giúp tập trung hóa dữ liệu chăm sóc khách hàng, thay thế cho việc nhận email rời rạc, hỗ trợ phân luồng và quản lý SLA (Service Level Agreement) nội bộ.
- **Các Actor tham gia:**
  - Guest: Khách vãng lai chưa có tài khoản.
  - Customer: Khách hàng đã đăng nhập.
  - Admin: Nhân viên chăm sóc khách hàng / Quản trị viên.
- **Luồng hoạt động tổng quát:** Actor (Guest/Customer) gửi Form liên hệ → Hệ thống lưu trữ trạng thái PENDING → Admin tiếp nhận trên Dashboard → Admin xử lý (gửi email phản hồi thủ công) → Đóng Ticket với trạng thái RESOLVED hoặc REJECTED.

## 2. ĐẶC TẢ CHI TIẾT BẢNG contact_messages

| Trường      | Ý nghĩa & Mục đích                                      | Sinh / Cập nhật / Không được cập nhật                  | Ràng buộc / Validation                                              | Ví dụ Hợp lệ / Không Hợp lệ            |
| :---------- | :------------------------------------------------------ | :----------------------------------------------------- | :------------------------------------------------------------------ | :------------------------------------- |
| message_id  | Khóa chính, định danh duy nhất của thư.                 | Sinh: Lúc Insert. Cập nhật: KHÔNG.                     | Tự tăng (IDENTITY).                                                 | Hợp lệ: 105                            |
| user_id     | Định danh khách hàng nếu đã Login. Để liên kết lịch sử. | Sinh: Từ JWT Token. Cập nhật: Khi xóa user (SET NULL). | NULL (Dành cho Guest). Nếu có, phải tồn tại trong bảng users.       | Hợp lệ: 55                             |
| guest_name  | Tên của khách vãng lai để xưng hô.                      | Sinh: Lúc Insert. Cập nhật: KHÔNG.                     | NOT NULL (Tầng logic) nếu user_id là NULL. Độ dài < 100.            | Hợp lệ: Nguyễn Văn A                   |
| guest_email | Email để Admin gửi thư phản hồi lại.                    | Sinh: Lúc Insert. Cập nhật: KHÔNG.                     | NOT NULL (Tầng logic) nếu user_id là NULL. Chuẩn định dạng Email.   | Không hợp lệ: abc@.com                 |
| subject     | Tiêu đề tóm tắt (Phân loại vấn đề).                     | Sinh: Lúc Insert. Cập nhật: KHÔNG.                     | NOT NULL. Giới hạn 200 ký tự. Cố định danh sách (Dropdown trên UI). | Hợp lệ: [Khiếu nại] Đơn hàng chưa giao |
| content     | Nội dung chi tiết lời nhắn.                             | Sinh: Lúc Insert. Cập nhật: KHÔNG.                     | NOT NULL. Tránh HTML injection. Min 10 ký tự.                       | Không hợp lệ: a                        |
| status      | Trạng thái xử lý của Ban quản trị.                      | Sinh: Mặc định PENDING. Cập nhật: Bởi Admin.           | Chỉ nhận 3 giá trị cố định.                                         | Hợp lệ: RESOLVED                       |
| created_at  | Thời điểm ghi nhận hệ thống.                            | Sinh: Database tự lấy giờ Server. Cập nhật: KHÔNG.     | Không cho phép client can thiệp.                                    | Hợp lệ: 2026-07-10 10:00:00+07         |

- **Giải thích CONSTRAINT chk_contact_status:** Bảo vệ tính toàn vẹn dữ liệu ở tầng vật lý (Database Level). Đảm bảo không có dòng code Backend nào bị lỗi có thể Insert các trạng thái rác như "PROCESSING", "DONE" vào DB.
- **Giải thích INDEX idx_contact_dashboard:** Composite Index này được tạo dựa trên hành vi truy vấn lõi của Admin: Lấy các thư chưa đọc (status = 'PENDING') và mới nhất xếp lên đầu (created_at DESC). Tránh Full-Table Scan khi dữ liệu lớn.

## 3. LUỒNG NGHIỆP VỤ CHI TIẾT (WORKFLOW)

**3.1. Luồng Gửi Liên hệ (Guest / Customer)**

1. **Frontend:** Render Form. Nếu chưa Login, hiển thị thêm ô Tên và Email. Cột subject sử dụng thẻ \<select\> (Dropdown) với các danh mục có sẵn.
2. **Backend (Spring Boot):** Kiểm tra SecurityContext.
   - Nếu có JWT → Lấy user_id, bỏ qua validate guest_name/email.
   - Nếu Anonymous → Bắt buộc validate guest_name và guest_email.
3. **Database:** Lưu bản ghi với status = 'PENDING'.
4. **Response:** Trả về mã HTTP 201 Created.

**3.2. Luồng Xử lý (Admin Dashboard)**

1. **Frontend:** Gọi API lấy danh sách tin nhắn.
2. **Backend:** SELECT \* FROM contact_messages WHERE status = ? ORDER BY created_at DESC LIMIT 20 OFFSET 0.
3. **Admin:** Xem chi tiết nội dung. Thực hiện hỗ trợ khách hàng (Gửi email giải quyết bằng hệ thống mail doanh nghiệp, hoặc gọi điện).
4. **Admin:** Bấm nút "Đánh dấu Đã xử lý" / "Từ chối".
5. **Database:** UPDATE contact_messages SET status = 'RESOLVED' WHERE message_id = ?.

## 4. QUY TẮC NGHIỆP VỤ (BUSINESS RULES)

1. **Tính Bất biến (Immutability):** Dữ liệu liên hệ là Bằng chứng giao tiếp. Người dùng KHÔNG ĐƯỢC phép sửa hoặc xóa lời nhắn sau khi đã nhấn gửi. Admin KHÔNG ĐƯỢC phép sửa nội dung (subject, content) của khách.
2. **Rule định danh:** Guest bắt buộc nhập Tên và Email. Customer (đã login) không cần nhập, Backend tự link tới bảng users qua user_id.
3. **Luật bảo toàn lịch sử:** Khi tài khoản khách hàng bị xóa khỏi bảng users, lịch sử liên hệ trong bảng contact_messages vẫn được giữ nguyên để phục vụ đối soát, cột user_id tự động chuyển thành NULL.
4. **State Machine (Chuyển trạng thái):** Trạng thái chỉ được đi từ PENDING → RESOLVED hoặc REJECTED. Không có chiều ngược lại.

## 5. VALIDATION VÀ DATA SANITIZATION

**Frontend (React UI):**

- guest_email: Regex chuẩn RFC 5322.
- subject: Bắt buộc chọn từ Dropdown List (Không cho gõ tự do).
- content: Trim() khoảng trắng hai đầu. Báo lỗi đỏ nếu < 10 ký tự.

**Backend (Spring Boot @Valid):**

- @NotBlank, @Size(max=200) cho Subject.
- @Email cho guest_email (Nếu Anonymous).
- **Chống XSS/HTML Injection:** Dùng thư viện HtmlUtils.htmlEscape(content) hoặc thư viện OWASP AntiSamy trước khi Insert vào DB để lọc thẻ \<script\>, \<iframe\> độc hại.

## 6. CHỐNG SPAM VÀ BẢO MẬT (SECURITY STRATEGY)

| Kịch bản Tấn công                 | Hậu quả                                            | Giải pháp tại Spring Boot & DB                                                                                                      |
| :-------------------------------- | :------------------------------------------------- | :---------------------------------------------------------------------------------------------------------------------------------- |
| **Spam gửi liên tục (Bot/Flood)** | Làm đầy Database, làm tắc nghẽn luồng xử lý Admin. | **Rate Limiting:** Sử dụng thư viện Bucket4j kết hợp Redis. Cấu hình: 1 IP / 1 user_id chỉ được gửi tối đa 3 request / 1 giờ.       |
| **Duplicate Submit (Bấm 2 lần)**  | Sinh ra 2 bản ghi giống hệt nhau cùng lúc.         | **Frontend:** Disable nút Submit ngay khi click. **Backend:** Bỏ qua request nếu có cùng subject + content + ip trong vòng 10 giây. |
| **SQL Injection**                 | Lộ lọt, xóa dữ liệu toàn bộ DB.                    | Sử dụng **Spring Data JPA / Hibernate**. Tự động áp dụng Parameterized Queries (PreparedStatement). Triệt tiêu 100% SQLi.           |
| **Gửi Email giả mạo**             | Admin gửi thư trả lời nhưng bị bounce (bật lại).   | Sử dụng Regex chặt chẽ ở DTO. Đối với hệ thống thật sẽ tích hợp email verify, nhưng ở phạm vi luận văn: Chấp nhận rủi ro này.       |

## 7. XỬ LÝ LỖI WORKFLOW (ERROR HANDLING MATRIX)

| Lỗi phát sinh                                         | Mã HTTP               | Thông báo cho Frontend                                     | Hành động của Hệ thống                                                              |
| :---------------------------------------------------- | :-------------------- | :--------------------------------------------------------- | :---------------------------------------------------------------------------------- |
| Guest không truyền Email                              | 400 Bad Request       | "Vui lòng nhập Email để chúng tôi phản hồi."               | Bắt lỗi tại @RestControllerAdvice, ném lỗi Validation.                              |
| Vượt quá giới hạn (Rate Limit)                        | 429 Too Many Requests | "Bạn đang thao tác quá nhanh, vui lòng thử lại sau 1 giờ." | Chặn ở tầng Filter/Interceptor, không chạm tới Database.                            |
| Cập nhật trạng thái đồng thời (Admin A và B cùng bấm) | 200 OK                | (Thành công bình thường)                                   | PostgreSQL xử lý Update cuối cùng thắng (Last Write Wins). Không gây lỗi nghiệp vụ. |

## 8. THIẾT KẾ REST API CONTRACTS

### 8.1. Khách hàng gửi liên hệ

- **Endpoint:** POST /api/v1/contacts
- **Auth:** Không bắt buộc (PermitAll).
- **Request Body:** {"subject": "Lỗi thanh toán", "content": "Tài khoản bị trừ...", "guestName": "Khôi", "guestEmail": "khoi@gmail.com"}
- **Logic BE:** Check JWT Header. Nếu có JWT → lấy user_id, set Name/Email = null. Nếu không JWT → Yêu cầu Name/Email.

### 8.2. Admin lấy danh sách

- **Endpoint:** GET /api/v1/admin/contacts?status=PENDING&page=0&size=20
- **Auth:** Bắt buộc Role ADMIN.
- **Response:** Trả về đối tượng Page\<ContactResponseDTO\>.

### 8.3. Admin Đổi trạng thái xử lý

- **Endpoint:** PATCH /api/v1/admin/contacts/{messageId}/status
- **Auth:** Role ADMIN.
- **Request Body:** {"status": "RESOLVED"}
- **Logic BE:** Verify messageId tồn tại. Kiểm tra DTO Status nằm trong tập enum hợp lệ. Gọi Repository.updateStatus().

## 9. MA TRẬN PHÂN QUYỀN (AUTHORIZATION)

- GUEST: Lọc ở lớp Filter. Chỉ được gọi POST /api/v1/contacts.
- CUSTOMER: Lọc qua JWT. Được gọi POST và GET /api/v1/contacts/me (Lịch sử của mình).
- ADMIN: Lọc qua Role hasRole('ADMIN'). Toàn quyền xem, lọc và đổi trạng thái trên các API có prefix /admin/\*.
