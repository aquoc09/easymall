# Báo cáo thay đổi Logic tự động sinh SKU Code

Báo cáo này tổng hợp lại các thay đổi liên quan đến việc loại bỏ logic ghép mã danh mục (Category Code) vào SKU tự động sinh, nhằm đảm bảo SKU hoàn toàn độc lập với danh mục của sản phẩm.

## 1. Lý do thay đổi
- **Vấn đề trước đây:** Mã SKU tự sinh bị phụ thuộc vào danh mục của sản phẩm. Việc trích xuất mã danh mục từ `categoryId` khá phức tạp (cắt dấu gạch ngang, giới hạn 3 ký tự) và có thể gây nhầm lẫn nếu danh mục thay đổi.
- **Mục tiêu:** Đồng nhất tiền tố của SKU tự sinh cho tất cả các sản phẩm thành một chuẩn chung (dùng tiền tố cố định `PRD`), không còn gắn liền với danh mục, giúp quản lý kho và SKU nhất quán hơn.

## 2. Các file bị ảnh hưởng

### [ProductServiceImpl.java](file:///d:/Study/DoAn/DATN/easymall/src/main/java/com/quocnva/easymall/service/product/impl/ProductServiceImpl.java)
- **Gỡ bỏ hàm `resolveCategoryCode`**: Hàm này trước đây dùng để truy vấn `categoryRepository` và format mã danh mục thành 3 chữ cái đầu. Đã bị xóa hoàn toàn.
- **Sửa đổi hàm `buildAndSaveVariants`**:
  - Không còn gọi `resolveCategoryCode` khi người dùng không truyền lên `skuCode`.
  - Đối với các biến thể (variants) đơn giản không có thuộc tính, SKU mới sẽ được nối chuỗi cứng theo định dạng: `PRD-{ProductId}-DEFAULT`.
  - Thay vì truyền `catCode` vào `SkuGenerator.generate`, giờ đây chỉ truyền `idForSku` và danh sách các thuộc tính biến thể.

### [SkuGenerator.java](file:///d:/Study/DoAn/DATN/easymall/src/main/java/com/quocnva/easymall/util/SkuGenerator.java)
- **Cập nhật hàm `generate`**: 
  - Gỡ bỏ tham số `String categoryCode` khỏi tham số đầu vào.
  - Sửa đổi chuỗi builder để nối cứng chữ `PRD` làm tiền tố thay cho việc nhận `categoryCode`.
- **Cập nhật Javadoc**: Đã sửa đổi comment tài liệu hóa để mô tả đúng format mới là `PRD-[PRODUCT_ID]-[ATTR1]-[ATTR2]-...`.

## 3. Cấu trúc SKU Code mới
Khi gọi API tạo/cập nhật sản phẩm, nếu không cung cấp cụ thể `skuCode`:
- **Trường hợp sản phẩm đơn giản (không chia size, màu sắc...):** 
  `PRD-{ProductId}-DEFAULT` 
  *(Ví dụ: PRD-15-DEFAULT)*
- **Trường hợp sản phẩm có chia thuộc tính (có màu sắc, size...):** 
  `PRD-{ProductId}-{Giá-trị-thuộc-tính-1}-{Giá-trị-thuộc-tính-2}` 
  *(Ví dụ: PRD-15-RED-XL)*

## 4. Kiểm thử
- Quá trình biên dịch (Maven build) đã hoàn tất thành công.
- Ứng dụng không bị dính lỗi liên quan tới Type/Arguments Mismatch sau khi sửa đổi chữ ký (signature) của hàm sinh SKU.

> [!TIP]
> Việc loại bỏ CategoryId khỏi logic sinh SKU giúp bạn dễ dàng cấu trúc lại cây danh mục của hệ thống trong tương lai mà không lo ảnh hưởng hoặc làm sai lệch ý nghĩa của kho mã SKU hiện tại.
