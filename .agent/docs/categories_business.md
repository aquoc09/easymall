Dưới đây là nội dung tài liệu **Business Category** đã được chuyển đổi sang định dạng Markdown (.md) để bạn dễ dàng lưu trữ và sử dụng:

# BUSINESS LOGIC CỐT LÕI (Nghiệp vụ Danh mục)

## A. Logic Lấy danh sách (Get Category Tree)

Bảng Danh mục là dữ liệu tĩnh ít thay đổi nhưng lại được truy vấn liên tục ở mọi trang (Header Menu, Sidebar Lọc sản phẩm). Do đó, hiệu năng là yếu tố sống còn.

- **Tuyệt đối không dùng N+1 Query**: Không dùng vòng lặp gọi Database để tìm quan hệ cha-con-cháu.
- **Xử lý tại tầng Java (O(N) Complexity)**: Backend thực hiện duy nhất một câu SQL `SELECT * FROM categories`. Sau đó, sử dụng HashMap trong Spring Boot để cấu trúc lại thành cây JSON (Tree Structure) trước khi trả về cho ReactTS hiển thị.
- **Hai luồng API tách biệt**:
  - **Public API (Cho UI Khách hàng)**: Chỉ lấy các danh mục có điều kiện `category_status = 1`.
  - **Private API (Cho UI Admin)**: Lấy tất cả danh mục, kèm theo các thông tin thống kê như `product_count` (số sản phẩm thuộc danh mục) để hỗ trợ quản lý.

## B. Logic Thêm mới (Create Category)

Admin Frontend chỉ cần gửi `categoryName` và `parentId` (nếu có). Backend sẽ tự động xử lý các phần còn lại:

- **Tự động hóa Level (Cấp độ)**:
  - Nếu `parent_id` là Null: Gán `level = 1` (Danh mục gốc).
  - Nếu có `parent_id`: Truy vấn danh mục cha và gán `level = level_cha + 1`.
- **Giới hạn độ sâu (Max Depth = 3)**: Hệ thống chỉ cho phép lồng tối đa 3 cấp để bảo vệ giao diện UI/UX trên Mobile không bị vỡ. Nếu Level \> 3, yêu cầu sẽ bị chặn.
- **Tự động sinh Code/Slug**: Sử dụng `SlugUtils` để chuyển `category_name` (ví dụ: "Đàn Guitar") thành `category_code` ("dan-guitar") phục vụ chuẩn SEO.

## C. Logic Cập nhật (Update Category)

- **Đổi Tên & Trạng Thái**: Cho phép thay đổi tên và bật/tắt trạng thái. Lưu ý không nên tự động đổi `category_code` khi đổi tên để tránh làm hỏng các liên kết đã được Google index.
- **Bật/Tắt Dây chuyền (Cascade Toggle)**: Khi ẩn một danh mục cha, hệ thống phải tự động đệ quy để ẩn toàn bộ các danh mục con và cháu thuộc nhánh đó.
- **Đổi Cha (Change Parent)**: Hiện tại, hệ thống **KHÔNG** cho phép đổi Parent để đảm bảo an toàn dữ liệu và tránh các lỗi phức tạp khi tính toán lại cây danh mục.

## D. Logic Xóa (Delete Category)

- **Xóa cứng (Hard Delete)**: Sử dụng lệnh `DELETE FROM categories`. Đây là thao tác nguy hiểm nên yêu cầu các vòng kiểm duyệt Validation cực kỳ khắt khe.

---

# 🛡️ 2. DANH SÁCH EXCEPTIONS VÀ BẮT LỖI (Validations)

Spring Boot sẽ ném ra các Exception (kèm HTTP Status 400 Bad Request) trong các trường hợp sau:

### 1\. Lỗi Dữ Liệu Đầu Vào (Input Validation)

- **CategoryNameBlankException**: Tên danh mục không được để trống.
- **DuplicateCategoryNameException**: Cảnh báo khi tên danh mục đã tồn tại để tránh trùng lặp.

### 2\. Lỗi Toàn Vẹn Dữ Liệu (Data Integrity)

- **CategoryNotFoundException**: Khi thực hiện Sửa/Xóa một ID không tồn tại trong DB.
- **ParentCategoryNotFoundException**: Khi `parent_id` truyền lên không có thật.

### 3\. Lỗi Cấu Trúc Cây (Tree Violations)

- **MaxLevelExceededException**: Cố tình thêm con vào danh mục đã ở Level 3.
- **CircularReferenceException**: Chặn trường hợp tạo vòng lặp vô hạn (ví dụ: A là cha của B, nhưng lại sửa để B làm cha của A).

### 4\. Lỗi Ràng Buộc Cơ Sở Dữ Liệu (Constraint Violations)

- **CategoryHasChildrenException**: Không cho phép xóa nếu danh mục vẫn còn chứa danh mục con.
- **CategoryInUseException**: Không cho phép xóa nếu đang có sản phẩm thuộc danh mục này.

Bạn có muốn tôi phác thảo cấu trúc Database chi tiết cho bảng danh mục này dựa trên logic trên không?
