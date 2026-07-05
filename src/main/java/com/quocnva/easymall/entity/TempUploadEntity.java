package com.quocnva.easymall.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * TempUploadEntity — "trạm trung chuyển" cho ảnh đã upload lên S3.
 *
 * <p>Vòng đời:
 * <ol>
 *   <li>Được tạo khi {@code POST /api/v1/uploads/image} thành công.</li>
 *   <li>Bị xóa khi URL được liên kết chính thức vào entity nghiệp vụ
 *       (ProductImageEntity, ReviewImageEntity, v.v.).</li>
 *   <li>Bị xóa tự động bởi {@code TempUploadCleanupService} nếu tồn tại
 *       quá {@code UploadConstants.CLEANUP_THRESHOLD_HOURS} giờ.</li>
 * </ol>
 */
@Entity
@Table(name = "temp_uploads")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TempUploadEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /** URL công khai của file trên S3 — duy nhất trong bảng. */
    @Column(name = "url", nullable = false, unique = true)
    private String url;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
