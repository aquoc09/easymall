# Quy Trình Tạo Resource Mới (Backend Workflow)

Để đảm bảo source code của dự án luôn `clean`, dễ mở rộng và tuân thủ chặt chẽ kiến trúc "Package by Layer" cũng như các nguyên tắc SOLID, đây là quy trình chuẩn để tạo một resource/module mới trong ứng dụng Spring Boot này.

## Bước 1: Database & Migration (Flyway)
- Định nghĩa cấu trúc bảng trong thư mục `src/main/resources/db/migration`.
- Đặt tên file theo chuẩn: `V<Version>__<Mô_tả>.sql` (Ví dụ: `V2.1__create_categories_table.sql`).
- Lưu ý: Sử dụng kiểu dữ liệu tối ưu, tạo Index cho các trường thường xuyên truy vấn/filter.

## Bước 2: Entity & Repository Layer
- **Entity**: Đặt tại `entity/`. 
  - Sử dụng các annotation JPA (`@Entity`, `@Table`, `@Id`, `@GeneratedValue(strategy = GenerationType.IDENTITY)`,...).
  - Sử dụng Lombok (`@Getter`, `@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@Builder`).
  - Mọi trường đại diện cho xoá mềm nên dùng `isActive` / `isDeleted`.
  - Tuyệt đối không để rò rỉ Entity ra ngoài Controller.
- **Repository**: Đặt tại `repository/`. 
  - Kế thừa `JpaRepository`. 
  - Khai báo các custom query (`@Query` hoặc Spring Data Method Naming) nếu cần thay vì thực hiện vòng lặp (N+1 query) trong service.

## Bước 3: DTOs (Data Transfer Objects)
- **Request DTO**: Đặt tại `dtos/request/`. 
  - Định nghĩa các model dùng để nhận dữ liệu từ Client. 
  - Sử dụng Jakarta Validation (`@NotBlank`, `@NotNull`, `@Size`,...) để validate đầu vào.
- **Response DTO**: Đặt tại `dtos/response/`. 
  - Định nghĩa format trả về, ẩn đi các field nhạy cảm hoặc không cần thiết.

## Bước 4: Exception Handling & Định Nghĩa Mã Lỗi
- Thêm mã lỗi mới vào `enums/ErrorCode.java`. Thiết lập `code`, `message`, `httpStatusCode`.
- Nếu có logic đặc thù (ví dụ: Quá số lượng level cho phép), cấu hình trong Service ném ra ngoại lệ `AppException(ErrorCode.XXX)`.
- GlobalExceptionHandler (`@RestControllerAdvice`) sẽ tự động catch và format thành JSON trả về.

## Bước 5: Service Layer (Business Logic)
- **Interface**: Đặt tại `service/`. Định nghĩa các phương thức chức năng.
- **Implementation**: Đặt tại `service/impl/`.
  - Phải dùng annotation `@Service` và `@RequiredArgsConstructor`.
  - Không bao giờ dùng `@Autowired` trực tiếp trên field. Dùng Dependency Injection qua Constructor (với `private final`).
  - Đặt annotation `@Transactional` cho các thao tác CUD (`INSERT/UPDATE/DELETE`), và `@Transactional(readOnly = true)` cho các thao tác đọc (`SELECT`).
  - Đây là nơi chứa 100% logic nghiệp vụ.

## Bước 6: Mapper Layer
- Sử dụng **MapStruct** tại `mapper/` để chuyển đổi qua lại giữa `Entity` và `DTO`.
- Khai báo interface với `@Mapper(componentModel = "spring")`.

## Bước 7: Controller Layer (REST API)
- Đặt tại `controller/`.
- Tiền tố API theo chuẩn: Dùng danh từ số nhiều, chữ thường, nối bằng dấu gạch ngang (VD: `/api/v1/food-items`).
- Mọi endpoint phải trả về qua class `ApiResponse<T>`. KHÔNG trả về trực tiếp `ResponseEntity<T>` hay Entity/DTO thuần.
  - Ví dụ thành công: `return ApiResponse.<T>builder().result(dto).build();`
- Gắn `@Valid` trước các Request Body.
- Tích hợp kiểm tra quyền truy cập qua `@PreAuthorize("@permissionChecker.has('...')")`.
- Controller chỉ làm vai trò định tuyến (Route), nhận request và trả response, tuyệt đối không chứa business logic.
