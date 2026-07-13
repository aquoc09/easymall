# Báo cáo Cập nhật Logic Query Sản phẩm

Báo cáo này tóm tắt các tính năng tìm kiếm, trích xuất và phân trang đối với danh sách sản phẩm (Product Querying) vừa được hoàn thiện.

## 1. Truy vấn Đệ quy qua nhiều Cấp Danh mục (Category Levels)
- **Vấn đề trước đây:** Hệ thống chỉ hỗ trợ việc tìm kiếm sản phẩm cho danh mục cha (Level 1) và các danh mục con trực tiếp (Level 2). Nếu cây danh mục sâu hơn, các sản phẩm nằm ở Level 3, 4 sẽ bị bỏ sót khi người dùng tìm kiếm theo danh mục cấp cao nhất.
- **Giải pháp đã triển khai:** 
  - Bổ sung hàm đệ quy `collectAllDescendantCategoryIds(Long parentId, List<Long> categoryIds)` bên trong `ProductServiceImpl`. 
  - Thay vì chỉ `findByParentId` 1 lần, hệ thống sẽ tự động quét đệ quy xuống tận cùng nhánh của cây danh mục để thu thập toàn bộ các `categoryId` con, cháu.
  - Sau đó gộp tất cả các IDs này vào truy vấn Specification `hasCategory`, đảm bảo sản phẩm ở bất kỳ cấp category con nào cũng sẽ được hiển thị khi xem danh mục gốc.

## 2. Tính năng Lọc Sản phẩm theo Kích thước (Filter by Size)
- Dữ liệu về `size`, `color` của sản phẩm đang được lưu bên trong mảng biến thể `ProductVariantEntity` dưới định dạng `JSONB` thông qua cột `variant_attributes` (VD: `{"size": "XL", "color": "RED"}`).
- **Giải pháp đã triển khai:**
  - Cập nhật DTO `ProductFilterRequest` với trường mới `String size`.
  - Bổ sung Specification `hasSize` trong `ProductSpecification`. Để lọc dữ liệu trong JSON hiệu quả trên MySQL, hệ thống sử dụng cấu trúc JOIN vào bảng `product_variants` và tận dụng hàm native `JSON_CONTAINS`.
  - Cụ thể, CriteriaBuilder được setup để sinh ra câu lệnh SQL tương đương với: `JSON_CONTAINS(variant_attributes, '{"size": "{Giá trị filter}"}')`. Điều này giúp đảm bảo hiệu năng database.

## 3. Quản lý Phân trang (Pagination)
- **Cơ chế hoạt động:** Chức năng phân trang đã được hệ thống Spring Boot (Spring Data JPA) xử lý một cách tự động thông qua class `Pageable`.
- **Luồng xử lý:**
  - Controller (ví dụ `getPublicProducts` / `getAllProducts`) sử dụng `@PageableDefault` để đón các param trên URL như `page`, `size` và `sort`. 
  - `Pageable` này được đẩy trực tiếp vào phương thức `findAll(spec, pageable)` của `ProductRepository`.
  - Spring Data tự động sinh thêm `LIMIT` và `OFFSET` vào câu query lấy sản phẩm, đồng thời thực thi thêm một câu lệnh `COUNT` ngầm định để tính tổng số phần tử (Total Elements) và tổng số trang (Total Pages).
  - Kết quả trả về cho Frontend (`Page<ProductResponse>`) đã có sẵn siêu dữ liệu (metadata) hoàn chỉnh để phục vụ việc render thanh chuyển trang.

> [!WARNING] 
> **Lưu ý về tên tham số (Parameter Naming):**
> Trong Spring MVC, `Pageable` mặc định sử dụng param tên là **`size`** trên URL để xác định *số lượng bản ghi trên một trang* (ví dụ: `?page=0&size=20`).
> Để tránh xung đột với cấu hình ngầm định của Spring, thuộc tính dùng để lọc kích cỡ sản phẩm trong `ProductFilterRequest` đã được thiết kế và đặt tên là **`variantSize`**.
> Vì vậy, khi Frontend gọi API để lọc kích cỡ áo/quần, hãy sử dụng param `?variantSize=M` thay vì `?size=M`.
