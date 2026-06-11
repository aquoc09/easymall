# Skill: Generate Full-Stack CRUD Feature

## Mục đích (Objective)
Tự động hóa việc tạo toàn bộ luồng CRUD (Create, Read, Update, Delete) cho một đối tượng (Entity) mới, bao gồm cả Backend (Spring Boot) và Frontend (ReactJS).

## Trigger (Khi nào sử dụng)
Áp dụng khi người dùng yêu cầu: "Tạo tính năng CRUD cho [Tên Đối Tượng]".

## Prerequisites (Yêu cầu bắt buộc)
1. LUÔN LUÔN đọc file `@backend-rules.md` và `@frontend-rules.md` trước khi bắt đầu.
2. LUÔN LUÔN kiểm tra file cấu trúc dữ liệu `@db-schema.md` để lấy chính xác các trường (fields) của [Tên Đối Tượng].

## Các bước thực hiện (Execution Steps)

### Phase 1: Backend Generation (Spring Boot)
Thực hiện tuần tự các bước sau trong thư mục `@backend`:
1. **Entity**: Tạo file `[EntityName]Entity.java` trong thư mục `entity/`. Bắt buộc có trường `isDeleted` (Soft Delete).
2. **DTOs**: Tạo `[EntityName]CreateRequest`, `[EntityName]UpdateRequest`, và `[EntityName]Response` trong thư mục `dtos/`.
3. **Repository**: Tạo `[EntityName]Repository.java` trong `repository/`.
4. **Service**:
   - Tạo interface `[EntityName]Service.java` trong `service/`.
   - Tạo class `[EntityName]ServiceImpl.java` trong `service/impl/`. Đảm bảo implement các hàm: `create`, `getById`, `getAll` (có phân trang), `update`, `delete` (soft delete).
5. **Controller**: Tạo `[EntityName]Controller.java` trong `controller/`. Bắt buộc sử dụng `@Valid` cho các request tạo/sửa.

### Phase 2: Frontend Generation (ReactJS)
Chờ cho đến khi Phase 1 hoàn tất, sau đó chuyển sang thư mục `@frontend`:
1. **API Service**: Tạo file `[entityName]Service.js` trong thư mục `src/services/` chứa các hàm gọi API bằng Axios (`getAll`, `getById`, `create`, `update`, `delete`).
2. **Custom Hook**: Tạo file `use[EntityName].js` trong `src/hooks/` để quản lý state loading, error và data khi gọi API.
3. **UI Components**: Trong thư mục `src/features/[entityName]/`:
   - Tạo `[EntityName]List.jsx`: Table/Grid hiển thị danh sách, có phân trang.
   - Tạo `[EntityName]Form.jsx`: Modal/Form để thêm mới và chỉnh sửa.

## Kiểm tra cuối cùng (Final Check)
- Đảm bảo Backend không trả về trực tiếp Entity.
- Đảm bảo Frontend gọi đúng endpoint vừa tạo ở Backend.
- Báo cáo cho người dùng bằng một bản tóm tắt ngắn gọn các file đã tạo.