### **🧠 1. BUSINESS LOGIC (NGHIỆP VỤ TÍCH HỢP GHN)**

Khi làm việc với GHN, chúng ta chia làm 2 mảng nghiệp vụ chính:

**A. Nghiệp vụ Master Data (Tỉnh/Huyện/Xã)**

- **Bản chất:** Dữ liệu này 10 năm mới đổi 1 lần.
- **Logic Bắt buộc:** BẮT BUỘC phải dùng bộ nhớ đệm (Cache). Nếu mỗi lần khách tạo Address bạn lại gọi API GHN, hệ thống sẽ bị "Rate Limit" (Khóa IP vì spam request) và khách hàng sẽ phải chờ rất lâu.
- **Chiến thuật:** Gọi API GHN 1 lần -&gt; Lưu thẳng vào RAM của Server (dùng Spring Cache + Caffeine) -&gt; Các lần sau lấy từ RAM ra trong 1 mili-giây.

**B. Nghiệp vụ Tính Phí Ship (Giao Hàng Nhanh Fee)**

- **Bản chất:** Dữ liệu động, phụ thuộc vào tuyến đường và cân nặng. Không được Cache.
- **Logic Tuyến Đường:** Tuyến Hà Nội -&gt; TP.HCM có thể có "Giao Hỏa Tốc", nhưng Hà Nội -&gt; Bản làng vùng sâu thì chỉ có "Giao Chuẩn".
- **Quy trình 2 bước:**
  1. Gọi API /available-services (Truyền Quận gửi + Quận nhận) để lấy danh sách gói cước hợp lệ (Lấy ra cái service_id hoặc service_type_id).
  2. Lấy cái service_id đó + Trọng lượng (gram) + Kích thước (dài x rộng x cao) ném vào API /fee để lấy ra con số VND cuối cùng.

---

### **🛡️ 2. DANH SÁCH EXCEPTIONS VÀ BẮT LỖI GHN**

Đặc thù của API GHN là **thường xuyên trả về HTTP Status 200 OK**, nhưng bên trong body JSON lại chứa code: 400 kèm thông báo lỗi. Do đó, bạn không thể dùng cơ chế bắt lỗi HTTP thông thường của Spring (như HttpClientErrorException) mà phải tự check cái code trong JSON.

Bạn cần tạo các Custom Exception sau:

1. **GhnIntegrationException (Base Exception - 500):** Lỗi chung khi không thể kết nối tới máy chủ GHN (Timeout, rớt mạng).
2. **GhnInvalidLocationException (400):** Khi Frontend gửi lên một cái districtId tào lao, gọi sang GHN bị báo lỗi "Không tìm thấy quận/huyện".
3. **GhnServiceUnavailableException (400):** Lỗi xảy ra khi gọi API tính phí mà tuyến đường đó GHN không hỗ trợ giao hàng.
