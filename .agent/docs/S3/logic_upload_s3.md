# Tài liệu Thiết kế Kiến trúc và Logic Nghiệp vụ Tích hợp AWS S3

Tài liệu này tổng hợp toàn bộ giải pháp kiến trúc và logic nghiệp vụ đã thảo luận để tích hợp dịch vụ lưu trữ AWS S3 vào hệ thống Backend sử dụng **Spring Boot** theo mô hình **Monolith phân rã Module**.

---

## 1. Tổng quan Kiến trúc Hệ thống

Hệ thống được xây dựng theo mô hình **Modular Monolith**, trong đó:

- **Module Upload (Shared Module):** Đóng vai trò là một dịch vụ dùng chung trong lòng hệ thống. Module này chịu trách nhiệm duy nhất là tiếp nhận tệp tin từ Frontend, giao tiếp với AWS S3 để lưu trữ. Các module khác không cần quan tâm đến cách thức kết nối hay cấu hình của S3.
- **Các Module Nghiệp vụ (Product, Review, User...):** Giao tiếp với Module Upload thông qua cơ sở dữ liệu chuỗi đường dẫn (URL String) được truyền từ Frontend để liên kết thông tin.

---

## 2. Luồng Nghiệp vụ Upload Ảnh (Luồng Tách biệt - Decoupled Flow)

Để tối ưu hóa trải nghiệm người dùng (UX) trên Frontend, luồng dữ liệu được thiết kế bóc tách làm 2 giai đoạn độc lập: Table: temp_uploads Columns: id, url, created_at

### Giai đoạn 1: Frontend tải ảnh lên hệ thống

1. Người dùng chọn một tệp ảnh trên giao diện.
2. Frontend lập tức gửi request chứa tệp tin đến Endpoint công khai của Module Upload.
3. Module Upload tiếp nhận, định danh lại tệp tin bằng một chuỗi duy nhất (UUID) để tránh trùng lặp tệp, sau đó đẩy trực tiếp lên **AWS S3 Bucket**.
4. Module Upload trả lại URL công khai của bức ảnh cho Frontend để hiển thị ảnh xem trước (Preview) cho người dùng.

### Giai đoạn 2: Lưu Form Nghiệp vụ chính

1. Người dùng điền xong các thông tin văn bản trên Form và nhấn nút "Lưu/Hoàn tất".
2. Frontend thu thập toàn bộ thông tin văn bản cùng với **URL của bức ảnh cuối cùng** (bỏ qua các URL ảnh cũ), gửi một request duy nhất đến Endpoint nghiệp vụ tương ứng.
3. Module Nghiệp vụ tiếp nhận request, tiến hành xử lý logic nghiệp vụ chính và liên kết URL ảnh đó vào thực thể tương ứng.

---

## 3. Giải pháp Quản lý Xử lý Ảnh Rác (Mô hình Trạm Trung Chuyển - Temp Table)

Do Frontend tải ảnh lên S3 trước khi người dùng lưu Form, sẽ xuất hiện các "ảnh mồ côi" (người dùng đổi ảnh hoặc thoát ngang). Để xử lý triệt để mà **không làm thay đổi cấu trúc của các Entity hiện tại** (như `ProductImageEntity` hay `ReviewImageEntity`), hệ thống áp dụng mô hình **Trạm Trung Chuyển (Dedicated Temp Table)**.

### 3.1. Khởi tạo Bảng Trung Chuyển

Hệ thống thiết lập một bảng dữ liệu độc lập (chỉ dùng nội bộ) đóng vai trò là "trạm chờ". Bảng này chỉ lưu trữ URL của tệp tin trên S3 và Thời gian tải lên. id (PK) url

created_at

### 3.2. Luồng Xử lý Dữ liệu 3 Bước

**Bước 1: Ghi nhận tạm thời (Khi gọi API Upload)**

- Ngay khi Module Upload nhận được URL từ S3, hệ thống tạo một bản ghi mới trong Bảng Trung Chuyển.
- Lúc này, bức ảnh được xem là đang ở trạng thái "tạm trú".

**Bước 2: Ghi nhận chính thức & Xóa tạm (Khi Lưu Form Nghiệp vụ)**

- Khi người dùng gửi Form (ví dụ: Tạo Sản phẩm), Module Nghiệp vụ sẽ lấy URL ảnh do FE gửi lên để lưu thẳng vào bảng thực thể chính (`ProductImageEntity`) như quy trình thông thường.
- **Logic then chốt:** Đồng thời với quá trình lưu chính thức, hệ thống sẽ truy vấn Bảng Trung Chuyển và **XÓA VĨNH VIỄN** bản ghi chứa URL tương ứng khỏi bảng này.
- _Kết quả:_ Bức ảnh đã có chủ sở hữu hợp lệ, Bảng Trung Chuyển được giải phóng.

**Bước 3: Dọn dẹp tự động (Scheduled Cleanup Job)**

- Sau Bước 2, Bảng Trung Chuyển lúc này chỉ còn sót lại URL của những bức ảnh bị người dùng "bỏ rơi" (không bao giờ được submit trong form chính).
- Một tác vụ tự động (Cronjob) được cấu hình chạy định kỳ vào thời điểm lưu lượng truy cập thấp. Tác vụ này sẽ quét toàn bộ Bảng Trung Chuyển.
- Nếu phát hiện bản ghi nào có Thời gian tải lên vượt quá giới hạn cho phép (ví dụ: tồn tại quá 24 giờ), hệ thống sẽ:
  1. Gọi API S3 để xóa vĩnh viễn tệp tin vật lý trên đám mây.
  2. Xóa bản ghi đó khỏi Bảng Trung Chuyển.

### 3.3. Ưu điểm của giải pháp này

- **Không xâm lấn kiến trúc:** Giữ nguyên vẹn 100% cấu trúc các Entity nghiệp vụ hiện có, giảm thiểu rủi ro khi refactor code.
- **Hiệu năng quét dọn cao:** Tác vụ Cronjob hoạt động cực kỳ nhẹ nhàng vì chỉ cần đọc trên một bảng phụ trợ duy nhất. Bảng này cũng duy trì dung lượng rất nhỏ do dữ liệu liên tục được dọn sạch ngay khi người dùng thao tác thành công.
