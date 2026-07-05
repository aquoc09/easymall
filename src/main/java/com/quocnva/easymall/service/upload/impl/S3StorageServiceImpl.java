package com.quocnva.easymall.service.upload.impl;

import com.quocnva.easymall.config.AwsS3Properties;
import com.quocnva.easymall.dtos.response.upload.UploadImageResponse;
import com.quocnva.easymall.entity.TempUploadEntity;
import com.quocnva.easymall.exception.AppException;
import com.quocnva.easymall.exception.ErrorCode;
import com.quocnva.easymall.repository.TempUploadRepository;
import com.quocnva.easymall.service.upload.StorageService;
import com.quocnva.easymall.util.UploadConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.ObjectCannedACL;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.net.URI;
import java.util.UUID;

/**
 * S3StorageServiceImpl — triển khai {@link StorageService} cho AWS S3.
 *
 * <p>Luồng upload:
 * <ol>
 *   <li>Validate content-type và kích thước file.</li>
 *   <li>Sinh S3 key theo pattern: {@code {folder}/UUID.{ext}}.</li>
 *   <li>PutObject lên S3 bucket.</li>
 *   <li>Lưu URL vào bảng {@code temp_uploads} (trạm trung chuyển).</li>
 *   <li>Trả về URL công khai.</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class S3StorageServiceImpl implements StorageService {

    private final S3Client s3Client;
    private final AwsS3Properties awsS3Properties;
    private final TempUploadRepository tempUploadRepository;

    @Override
    @Transactional
    public UploadImageResponse uploadImage(MultipartFile file, String folder) {
        validateFile(file);

        String bucketName = awsS3Properties.getS3().getBucketName();
        String s3Key = buildS3Key(folder, file.getOriginalFilename());

        putObjectToS3(file, bucketName, s3Key);

        // Build URL từ aws.base-url đã cấu hình trong application.yaml
        String publicUrl = awsS3Properties.getBaseUrl() + "/" + s3Key;

        // Ghi nhận vào bảng trung chuyển
        tempUploadRepository.save(TempUploadEntity.builder()
                .url(publicUrl)
                .build());

        log.info("Uploaded image to S3: key={}, url={}", s3Key, publicUrl);
        return UploadImageResponse.builder().url(publicUrl).build();
    }

    @Override
    @Transactional
    public void deleteByUrl(String url) {
        String bucketName = awsS3Properties.getS3().getBucketName();
        String s3Key = extractKeyFromUrl(url);

        s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .build());

        log.info("Deleted object from S3: key={}", s3Key);
    }

    // ══════════════════════════════════════════════════════════════════
    // Private helpers
    // ══════════════════════════════════════════════════════════════════

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new AppException(ErrorCode.EMPTY_FILE);
        }
        String contentType = file.getContentType();
        if (contentType == null || !UploadConstants.ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new AppException(ErrorCode.INVALID_FILE_TYPE);
        }
        if (file.getSize() > UploadConstants.MAX_FILE_SIZE_BYTES) {
            throw new AppException(ErrorCode.FILE_TOO_LARGE);
        }
    }

    /**
     * Sinh S3 key: {@code {folder}/UUID.{ext}}.
     * UUID đảm bảo không bị trùng tên dù nhiều user upload cùng lúc.
     */
    private String buildS3Key(String folder, String originalFilename) {
        String extension = extractExtension(originalFilename);
        return folder + "/" + UUID.randomUUID() + extension;
    }

    private String extractExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "";
        return filename.substring(filename.lastIndexOf("."));
    }


    private void putObjectToS3(MultipartFile file, String bucket, String key) {
        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(file.getContentType())
                    .contentLength(file.getSize())
                    .acl(ObjectCannedACL.PUBLIC_READ)
                    .build();

            s3Client.putObject(request, RequestBody.fromBytes(file.getBytes()));
        } catch (IOException e) {
            log.error("Failed to read file bytes for S3 upload: {}", e.getMessage());
            throw new AppException(ErrorCode.S3_UPLOAD_FAILURE);
        } catch (Exception e) {
            log.error("S3 upload failed: {}", e.getMessage());
            throw new AppException(ErrorCode.S3_UPLOAD_FAILURE);
        }
    }

    /**
     * Trích xuất S3 key từ URL công khai.
     * Ví dụ: {@code https://bucket.s3.region.amazonaws.com/products/uuid.jpg}
     * → {@code products/uuid.jpg}
     */
    private String extractKeyFromUrl(String url) {
        try {
            URI uri = URI.create(url);
            String path = uri.getPath();
            // path có dạng "/products/uuid.jpg" — bỏ dấu "/" đầu
            return path.startsWith("/") ? path.substring(1) : path;
        } catch (Exception e) {
            log.warn("Cannot extract S3 key from URL: {}", url);
            return url;
        }
    }
}
