# Session Analysis Report - Slider Management Module

**Date:** 2026-07-10  
**Module:** Slider (Quản lý Banner/Slider)  
**Status:** ✅ COMPLETED

---

## 1. Mục Tiêu Phiên Làm Việc

Thiết kế và triển khai đầy đủ kiến trúc API Backend quản lý Slider (Banner) để cung cấp dữ liệu cho trang chủ. Đảm bảo tuân thủ tiêu chuẩn code của dự án bao gồm: Entity, DTO, Mapper (với MapStruct), Repository, Service, và Controller. Cung cấp API Public và Admin phân quyền riêng biệt.

---

## 2. Quyết Định Kiến Trúc Quan Trọng

### 2.1 Tái Sử Dụng API Upload
**Quyết định:** Không implement logic upload file ảnh vào trực tiếp API Create/Update Slider.
**Lý do:** Tận dụng Module Upload chung đã có sẵn trong dự án. Client sẽ gọi upload trước, lấy URL dạng chuỗi và gửi vào API `SliderCreateRequest`. Backend chỉ xử lý chuỗi URL. Điều này làm giảm sự phức tạp của Controller và tái sử dụng logic xử lý tệp.

### 2.2 Xử lý `storage.base-url` qua MapStruct
**Quyết định:** Tận dụng tính năng `@AfterMapping` của MapStruct (`SliderMapper`) để kiểm tra nếu `imageUrl` được lưu không phải là full link (không bắt đầu bằng "http"), thì sẽ tự động gắn thêm prefix `storageBaseUrl` (được inject bằng `@Value("${storage.base-url}")`) trước khi trả về `SliderResponse`.

### 2.3 Danh Sách Public Đơn Giản
**Quyết định:** Đối với endpoint `GET /api/v1/sliders/public`, Backend chỉ lọc theo `isActive = true` và sort `displayOrder ASC` rồi trả về `List<SliderResponse>` tĩnh (không có phân trang).
**Lý do:** Dữ liệu Slider thường có số lượng ít (< 10 records) được render ở màn hình chính, không cần thiết phải dùng Pageable gây dư thừa dữ liệu meta và truy vấn count không cần thiết.

---

## 3. Các Files Đã Tạo / Chỉnh Sửa

| File | Trạng thái | Ghi chú |
|---|---|---|
| `entity/SliderEntity.java` | ✅ Mới | JPA Entity ánh xạ bảng `sliders` theo schema Flyway `V2.4` |
| `repository/SliderRepository.java` | ✅ Mới | Kèm custom method `findByIsActiveTrueOrderByDisplayOrderAsc` |
| `dtos/request/slider/SliderCreateRequest.java` | ✅ Mới | Request validate bắt buộc field `imageUrl` |
| `dtos/request/slider/SliderUpdateRequest.java` | ✅ Mới | Partial update request |
| `dtos/response/slider/SliderResponse.java` | ✅ Mới | Map response theo chuẩn DTO |
| `mapper/SliderMapper.java` | ✅ Mới | MapStruct `componentModel="spring"` + `storage.base-url` append |
| `service/slider/SliderService.java` | ✅ Mới | Abstract Service interface |
| `service/slider/impl/SliderServiceImpl.java` | ✅ Mới | 100% Business Logic, ném ngoại lệ chuẩn |
| `controller/SliderController.java` | ✅ Mới | 5 endpoints (1 Public, 4 Admin kèm auth guards) |
| `exception/ErrorCode.java` | ✅ Chỉnh sửa | Bổ sung mã lỗi `16001 (SLIDER_NOT_FOUND)` |
| `resources/messages.properties` | ✅ Chỉnh sửa | Thêm các translate messages cho Slider |
| `db/migration/V6.0__seed_slider_permissions.sql` | ✅ Mới | Script tự động insert permissions cho role Admin |

---

## 4. Business Logic chi tiết đã Implement

### 4.1 Phân tách Public vs Admin API
| Endpoint | Auth | Role Required | Dữ liệu trả về |
|---|---|---|---|
| `GET /api/v1/sliders/public` | Không | Không | Dữ liệu List Slider có `isActive=true` |
| `GET /api/v1/sliders` | Có | `slider:read` | Trả về tất cả Slider kèm **Phân Trang** |
| `POST /api/v1/sliders` | Có | `slider:create` | Tạo mới, mặc định `isActive=true`, `displayOrder=0` |
| `PUT /api/v1/sliders/{id}` | Có | `slider:update` | Cập nhật một phần (Patch) bất kì field nào |
| `DELETE /api/v1/sliders/{id}` | Có | `slider:delete` | Xóa cứng (Hard Delete) khỏi Database |

### 4.2 Seed Permissions Validation
Tạo mới script `V6.0` để bảo đảm mỗi lần boot, Flyway sẽ seed data quyền hạn an toàn (`ON CONFLICT DO NOTHING`) vào 2 bảng `permissions` và `role_permissions` để Role ID 1 (Admin) có toàn quyền.

---

## 5. Error Codes đã Thêm Mới

| Code | Key | HTTP Status | Mô tả |
|---|---|---|---|
| 16001 | `SLIDER_NOT_FOUND` | 404 | Slider không tồn tại |

---

## 6. Điểm Cần Chú Ý & TODO Trong Tương Lai

- Cần bổ sung Postman endpoints cho Slider module nếu dự án chia sẻ thông tin request bằng Postman Collection chung.
- Xóa mềm (Soft Delete) chưa được áp dụng vì theo schema bảng `sliders` không có `is_deleted`. Đang sử dụng Hard Delete.
- Hiện không có bảng kiểm tra tính hợp lệ của `imageUrl` do Client truyền vào (ví dụ kiểm tra đuôi `.png`, `.jpg`). Chức năng này được đặt niềm tin hoàn toàn vào module Upload.
