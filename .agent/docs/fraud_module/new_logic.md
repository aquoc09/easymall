PHẦN 2: PHÂN HỆ KIỂM SOÁT RỦI RO & GIAN LẬN (FRAUD DETECTION RULE ENGINE)
Định vị nghiệp vụ: Đây là "Vũ khí hạng nặng" mang tính thực tiễn cao, ghi điểm tuyệt đối trước Hội đồng vì nó thể hiện tư duy thiết kế hệ thống vận hành thực tế (Enterprise Architecture). Hệ thống sử dụng kiến trúc Rule-based Engine kết hợp xử lý bất đồng bộ (Event-Driven), giúp chủ động ngăn chặn các rủi ro kinh doanh mà không làm ảnh hưởng đến trải nghiệm mua sắm (Latency) của người dùng.
2.1. Mục đích và Vai trò
Vai trò: Hoạt động như một "Màng lọc bảo mật" ngầm (Background Job / Event Listener) quét qua các giao dịch và hành vi của người dùng trên hệ thống thông qua các bộ quy tắc (Rules) được định nghĩa sẵn.
Mục đích: Đánh giá rủi ro (Risk Assessment) và chủ động phát hiện các luồng hành vi bất thường (Spam đặt hàng, dùng thẻ tín dụng chùa, tạo tài khoản ảo để trục lợi khuyến mãi, bom hàng) và cấu thành các Cảnh báo (Alerts) cho Admin trước khi hàng hóa được xuất kho vật lý.
Kiến trúc lõi: Dựa trên tập dữ liệu động từ bảng risk*rule_configs (Rule Configuration) để cấu thành logic kiểm tra (Rule Evaluation) và ghi nhận kết quả vào bảng risk_alerts (Alert Generation).
2.2. Đặc tả Bảng Cấu hình Quy tắc (risk_rule_configs)
Vai trò: Là "Trái tim" của Fraud Engine. Bảng này lưu trữ toàn bộ các tham số cấu hình của các quy tắc gian lận. Hệ thống Rule Engine sẽ dựa vào các thông số tại đây để quyết định xem một hành vi có vi phạm hay không.
2.2.1. Phân tích Trường dữ liệu (Data Dictionary)
Trường (Column)
Ý nghĩa & Mục đích
Phân quyền & Giá trị
rule_code
Mã quy tắc (Khóa chính). Định danh duy nhất để code Backend map với logic xử lý. Định dạng: R[Số]*[TÊN_QUY_TẮC].
System Developer (Cố định)
rule_name
Tên hiển thị giúp Admin/Risk Manager hiểu quy tắc này dùng để làm gì.
Admin có thể sửa
risk_level
Mức độ rủi ro khi vi phạm. Gồm: LOW, MEDIUM, HIGH, CRITICAL. Quyết định mức độ ưu tiên xử lý của Admin.
Admin có thể sửa
threshold_value
Ngưỡng vi phạm. Là con số định lượng (Số tiền, Số lần...). Ví dụ: 3 (lần), 5000000 (VNĐ).
Admin có thể sửa
time_window_minutes
Khung thời gian quét dữ liệu (Rolling Window). Đơn vị tính: Phút. Ví dụ: 10 (10 phút), 1440 (24 giờ).
Admin có thể sửa
is_active
Trạng thái bật/tắt quy tắc (TRUE/FALSE).
Admin có thể sửa
updated_at
Dấu vết thời gian cập nhật cấu hình gần nhất.
Tự động (System)

2.2.2. Logic Nghiệp vụ (Business Rules)
Khi nào đọc dữ liệu: Hệ thống Backend sẽ load toàn bộ bảng này vào RAM/Cache (như Redis hoặc Caffeine) lúc khởi động ứng dụng (Startup) hoặc mỗi khi có thay đổi. Rule Engine luôn đọc từ Cache để đảm bảo độ trễ (Latency) khi Evaluate < 10ms.
Luồng Bật/Tắt Rule (is_active): Khi diễn ra các chiến dịch lớn (Flash Sale, 11/11), Admin có thể chủ động chuyển is_active = FALSE đối với một số rule khắt khe để tránh chặn nhầm khách hàng thật, hệ thống sẽ bỏ qua rule này trong luồng Evaluate.
Cập nhật Ngưỡng (threshold_value): Tương tự, Admin có thể nới lỏng ngưỡng báo động trực tiếp trên giao diện (Ví dụ: Tăng ngưỡng R5_NEW_ACC_HIGH_VALUE từ 5 triệu lên 10 triệu) mà không cần deploy lại code.
Luồng thêm Rule mới (Extensibility): Được thiết kế theo nguyên tắc Open-Closed Principle. Trong tương lai, chỉ cần thêm 1 dòng data mới vào bảng này (VD: R6_MULTIPLE_VOUCHERS), và viết thêm 1 class RuleEvaluator implements interface chuẩn ở Backend, hệ thống tự động nhận diện Rule mới mà tuyệt đối không cần thay đổi cấu trúc Database.
2.3. Đặc tả Bảng Cảnh báo Rủi ro (risk_alerts)
Vai trò: Sổ cái lưu trữ các cảnh báo do Rule Engine sinh ra. Đây là màn hình làm việc chính của bộ phận Vận hành/Kiểm soát rủi ro.
2.3.1. Các mối quan hệ (Relationships)
Với User (user_id): Cảnh báo sinh ra thuộc về User nào.
Với Order (order_id): Cảnh báo này gắn với Đơn hàng nào (Có thể NULL nếu hành vi vi phạm không nằm ở bước đặt hàng, ví dụ: Đăng nhập sai pass quá nhiều lần).
Với Rule (rule_code): Vi phạm quy tắc nào trong bảng risk_rule_configs.
2.3.2. Vòng đời Trạng thái (status)
PENDING: Trạng thái mặc định khi Rule Engine vừa sinh ra Alert. Đang chờ con người xử lý. Đơn hàng liên kết sẽ bị Tạm khóa (Không cho phép chuyển sang trạng thái Đang giao).
REVIEWING: Admin đã click vào xem chi tiết cảnh báo và đang trong quá trình điều tra (gọi điện xác minh, check lịch sử IP).
RESOLVED: Admin xác nhận đây đúng là Gian lận. Alert được đóng, Đơn hàng bị Hủy (Cancelled) và Tài khoản có thể bị Ban.
FALSE_POSITIVE: (Báo động giả) Admin xác nhận khách hàng hợp lệ (Ví dụ: Khách mua sỉ thực sự). Alert được đóng, Đơn hàng được Mở khóa để tiếp tục quy trình Giao hàng.
2.3.3. Điều kiện sinh và Không sinh Alert (De-duplication)
Khi nào sinh Alert: Khi kết quả Evaluate của một Rule trả về kết quả vượt ngưỡng threshold_value trong khoảng thời gian time_window_minutes.
Luồng Chống trùng lặp (No Spam): Trước khi INSERT vào risk_alerts, hệ thống phải kiểm tra: Nếu cùng một order_id VÀ cùng một rule_code đã tồn tại một Alert có trạng thái PENDING hoặc REVIEWING, hệ thống sẽ bỏ qua không sinh thêm Alert mới để tránh gây nhiễu cho Admin.
2.4. Sơ đồ Kiến trúc & Luồng xử lý Fraud Rule Engine
2.4.1. Vị trí của Rule Engine trong hệ thống (High-level Architecture)
Kiến trúc đảm bảo việc đánh giá gian lận không chặn đứng luồng Checkout chính, mà chạy ngầm (Background/Event-Driven) ngay sau khi đơn hàng hình thành.
Plaintext
[ Khách hàng ]
│ (1) Checkout
▼
[ Order Service ] ───(2) Đặt hàng thành công ───> [ Database (orders) ]
│
│ (3) Bắn Event: OrderCreatedEvent (Kafka / Spring Event)
▼
╔══════════════════════════════════╗
║ FRAUD RULE ENGINE ║
║ ║
║ ├── Đọc Active Rules từ Cache ║
║ ├── Evaluate R1_MULTIPLE_DEVICES ║
║ ├── Evaluate R2_FAILED_PAYMENTS ║
║ ├── Evaluate R5_NEW_ACC_HIGH... ║
║ └── Evaluate Rule (n) ║
╚══════════════════════════════════╝
│ (4) Phân tích kết quả
▼
[ Cấu thành Risk Alerts ]
│
┌──────┴──────┐
(5a) Có vi phạm (5b) Không vi phạm
▼ ▼
[ Flag for Review ] [ Continue Normal Flow ]
(Insert Alert) (Chờ Xử lý / Giao hàng)

2.4.2. Trình tự Đánh giá Rule (Rule Evaluation Logic)
Plaintext
Bắt đầu Evaluate (1 Sự kiện)
│
▼
Lấy danh sách tất cả Rules đang Active
│
▼
┌────────> Kiểm tra Rule 1 ──────────┐
│ │
[ Không đạt ] [ Đạt điều kiện ]
│ │
▼ ▼
Kiểm tra Rule 2 <── [ Tiếp tục ] ─── Sinh Risk Alert 1
│  
 ▼  
 [ Không đạt ] [ Đạt điều kiện ]
│ │
▼ ▼
Kiểm tra Rule n <── [ Tiếp tục ] ─── Sinh Risk Alert 2
│
▼
Kết thúc (Lưu danh sách Alerts vào Database)

2.5. Đặc tả Use Case Quản trị (Admin)
Use Case 1: Tùy biến Quy tắc Gian lận (Rule Configuration)
Trigger: Admin cần thắt chặt hoặc nới lỏng cơ chế phòng vệ dựa theo tình hình kinh doanh.
Hành động:
PUT /api/v1/admin/risk-rules/{rule_code}
Admin thay đổi threshold_value (Ngưỡng), time_window_minutes (Thời gian) hoặc is_active (Bật/tắt).
Post-condition: Cấu hình mới lưu vào DB và lập tức Sync lên Cache để các đơn hàng tiếp theo áp dụng luật mới ngay lập tức.
Use Case 2: Xử lý Đơn hàng bị cắm cờ (Alert Resolution)
Trigger: Admin nhận được thông báo (WebSocket/FCM) có Alert mới.
Hành động:
GET /api/v1/admin/risk-alerts: Xem danh sách (Lọc theo status = PENDING).
Admin click xem chi tiết Đơn hàng & User.
POST /api/v1/admin/risk-alerts/{alert_id}/resolve: Xác nhận đánh giá.
Luồng phụ:
Nếu payload gửi lên là status = RESOLVED: Hệ thống kích hoạt Job Hủy đơn hàng và gửi Email báo vi phạm cho khách.
Nếu payload là status = FALSE_POSITIVE: Hệ thống cập nhật trạng thái đơn hàng từ (Tạm giữ) sang (Chờ lấy hàng).
2.6. Khả năng mở rộng trong tương lai (Scalability & Extensibility)
Tài liệu này khẳng định thiết kế Data-Driven Rule Engine. Danh sách Rule (R1, R2, R5...) chỉ là bộ quy tắc khởi tạo (Default Seed Data).
Hệ thống được thiết kế hoàn toàn tách biệt (Decoupled) giữa Dữ liệu cấu hình (DB) và Logic đánh giá (Code).
Trong tương lai, để bổ sung các hình thái gian lận mới (Ví dụ: Phát hiện 1 IP đăng ký 100 tài khoản trong 1 giờ để gom mã giảm giá), team Vận hành chỉ cần:
Định nghĩa Rule mới vào bảng risk_rule_configs (Ví dụ: R6_MASS_REGISTER_IP, Threshold: 100, Window: 60 phút).
Viết thêm 1 Component độc lập tại Backend thực thi interface kiểm tra logic đếm IP.
Không cần can thiệp vào Core Checkout, không cần ALTER bảng trong Database, đảm bảo tuân thủ kiến trúc Microservices và dễ dàng bảo trì.
