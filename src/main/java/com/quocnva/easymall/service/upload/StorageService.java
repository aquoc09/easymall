package com.quocnva.easymall.service.upload;

import com.quocnva.easymall.dtos.response.upload.UploadImageResponse;
import org.springframework.web.multipart.MultipartFile;

/**
 * StorageService — contract cho việc upload/xóa file trên cloud storage.
 *
 * <p>Các module nghiệp vụ (Product, Review) KHÔNG gọi trực tiếp S3 mà
 * thông qua interface này, đảm bảo dễ mock trong test và dễ thay thế
 * provider (S3 → GCS, Azure Blob...) mà không cần sửa code nghiệp vụ.
 */
public interface StorageService {

    /**
     * Upload một file ảnh lên cloud storage.
     *
     * @param file   file từ multipart request
     * @param folder folder prefix trên S3 (vd: "products", "reviews")
     * @return response chứa URL công khai
     */
    UploadImageResponse uploadImage(MultipartFile file, String folder);

    /**
     * Xóa file khỏi S3 theo URL công khai.
     * Dùng trong cleanup job để xóa ảnh "mồ côi".
     *
     * @param url URL công khai đầy đủ của file trên S3
     */
    void deleteByUrl(String url);
}
