# 1. BUSINESS LOGIC CỐT LÕI (Nghiệp vụ Slider & Campaign)

### A. Logic Hiển thị (Read - Get Data)

Vì cấu trúc đã tách làm bảng cha (Campaign) và bảng con (Slider), hệ thống cần các luồng truy xuất tách biệt để đảm bảo hiệu năng và bảo mật.

**1. API cho Khách hàng (Public API - Phục vụ màn hình Trang chủ):**

- **Mục đích:** Chỉ trả về danh sách ảnh của sự kiện **ĐANG DIỄN RA** hôm nay.
- **Điều kiện lọc (Filters):**
  - Trạng thái: Bảng `slider_campaigns.is_active` = TRUE VÀ bảng `sliders.is_active` = TRUE.
  - Logic Thời gian chu kỳ (Time-bound Logic): Lấy Ngày và Tháng hiện tại của server so sánh với chu kỳ của sự kiện. Backend xử lý 2 trường hợp:
    - _Sự kiện trong năm (start_month <= end_month):_ Thời gian hiện tại phải nằm kẹp giữa ngày/tháng bắt đầu và ngày/tháng kết thúc.
    - _Sự kiện vắt chéo năm (start_month > end_month - ví dụ 20/12 đến 10/01):_ Logic OR thông minh để quét các ngày cuối năm ngoái hoặc đầu năm nay.
- **Sắp xếp:** BẮT BUỘC `ORDER BY sliders.display_order ASC` để Swiper trên ReactTS trượt đúng thứ tự.

**2. API cho Admin (Private API - Phục vụ màn hình Quản trị):**

- **Mục đích:** Trả về toàn bộ chiến dịch và slider (kể cả đang bị tắt) để quản lý.
- **Dữ liệu trả về bổ sung (Computed Fields):** Backend tính toán và gửi thêm một cờ `is_currently_running` = TRUE/FALSE (Chiến dịch này có đang rơi vào ngày hôm nay không) để ReactTS hiển thị nhãn "Đang chạy" (Màu xanh) hoặc "Chờ đến mùa" (Màu xám).

### B. Logic Thêm mới (Create)

**1. Thêm mới Nhóm Sự kiện (Create Campaign):**

- Frontend gửi JSON chứa `campaignName`, `startDay`, `startMonth`, `endDay`, `endMonth`, `isActive`.

**2. Thêm mới Banner vào Sự kiện (Create Slider):**

- Frontend gửi JSON: `campaignId`, `imageUrl`, `targetUrl`, `displayOrder`, `isActive`.
- **Auto-Order Logic (Tự động xếp hạng):** Nếu Admin không truyền `display_order`, Backend tự query lấy số `display_order` lớn nhất thuộc `campaign_id` đó rồi cộng thêm 1.
- **Shift-Down Logic (Thuật toán Đẩy lùi):** Nếu Admin cố tình truyền một `display_order` đã tồn tại (ví dụ vị trí số 1), Backend tự động thực hiện lệnh UPDATE cộng 1 (+1) vào thứ tự của tất cả các banner cũ đang đứng từ vị trí số 1 trở về sau, tạo khoảng trống để nhét banner mới lên đầu một cách mượt mà.

### C. Logic Cập nhật (Update) & Cập nhật nhanh Trạng thái

**1. Cập nhật thông tin toàn bộ (PUT):**

- Admin có thể sửa URL ảnh hoặc sửa ngày tháng chu kỳ sự kiện. Cập nhật ngày ở bảng cha sẽ tự động áp dụng thời gian hiển thị mới cho toàn bộ slider con bên trong.

**2. Cập nhật nhanh (Toggle Active - PATCH):**

- **Cắt cầu dao tổng:** Cập nhật `is_active` ở bảng `slider_campaigns`. Ẩn toàn bộ sự kiện.
- **Tắt lẻ tẻ:** Cập nhật `is_active` ở bảng `sliders`. Chỉ ẩn 1 banner bị lỗi chính tả mà không ảnh hưởng các banner khác trong cùng chiến dịch.

**3. Đổi vị trí kéo thả (Reorder Sliders - PUT):**

- Phục vụ tính năng Drag & Drop trên ReactTS. FE chỉ cần gửi lên một mảng các `slider_id` đã được sắp xếp lại: [5, 2, 8, 1].
- Backend nhận mảng, dùng vòng lặp cập nhật lại cột `display_order` theo đúng chỉ mục (Index) của mảng (ví dụ `slider_id` = 5 gán order = 1, `slider_id` = 2 gán order = 2...).

### D. Logic Xóa (Delete)

- **Xóa Cứng (Hard Delete):** Bảng này chứa dữ liệu hiển thị tĩnh, không dính líu đến đơn hàng hay doanh thu. Admin bấm xóa là chạy lệnh `DELETE FROM sliders`.
- **Xóa Dây chuyền (Cascade):** Nếu Admin xóa bảng cha `slider_campaigns`, cơ sở dữ liệu sẽ tự động dọn sạch các record con trong bảng `sliders` nhờ khóa ngoại `ON DELETE CASCADE`.
- **Dọn rác Cloud (Clean-up):** Trước khi xóa record trong DB, Spring Boot kích hoạt một luồng phụ (Async) gọi API sang Cloudinary/S3 để xóa file ảnh vật lý, chống đầy rác server.

---

## 🛡️ 2. DANH SÁCH EXCEPTIONS VÀ VALIDATIONS (Bắt lỗi)

Để hệ thống chống chịu được mọi dữ liệu rác từ phía Client, các Data Transfer Object (DTO) trong Spring Boot bắt buộc phải có các Validation sau:

### 1. Lỗi Dữ Liệu Đầu Vào (Input Validation)

- **ImageUrlBlankException** (Sử dụng @NotBlank): Link ảnh không được để trống.
- **InvalidUrlFormatException** (Sử dụng @URL): `image_url` và `target_url` bắt buộc phải là định dạng đường dẫn web (HTTP/HTTPS) hợp lệ. Ngăn chặn hacker truyền mã script độc hại.
- **DisplayOrderNegativeException** (Sử dụng @Min(0)): Thứ tự hiển thị không được là số âm.
- **InvalidDayMonthException** (Sử dụng @Min và @Max):
  - `start_month` và `end_month` bắt buộc từ 1 đến 12.
  - `start_day` và `end_day` bắt buộc từ 1 đến 31.
  - _(Nâng cao)_: Backend kết hợp kiểm tra logic ngày của tháng (Ví dụ: Chặn nhập tháng 2 ngày 30).

### 2. Lỗi Toàn Vẹn Cấu Trúc Thời Gian (Time Integrity)

- **SameDayCampaignException**: Ném lỗi nếu `start_day` == `end_day` VÀ `start_month` == `end_month`. Một chiến dịch quảng cáo không thể sinh ra và kết thúc trong cùng 1 ngày, tối thiểu phải kéo dài sang ngày hôm sau.

### 3. Lỗi Toàn Vẹn Dữ Liệu (Data Integrity)

- **DuplicateCampaignNameException**: Khi tạo chiến dịch mới, tên sự kiện (ví dụ: "Sale Mùa Hè") bị trùng với tên đã có trong DB (Dựa vào ràng buộc UNIQUE).
- **CampaignNotFoundException**: Khi thao tác thêm Slider vào một `campaign_id` không tồn tại.
- **SliderNotFoundException**: Khi Admin gọi API Cập nhật, Đổi vị trí, Toggle trạng thái hoặc Xóa một `slider_id` không có thực trong hệ thống.
