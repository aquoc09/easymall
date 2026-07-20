# Sequence Diagrams for Upload Service

Tài liệu này chứa các sơ đồ tuần tự cho các hoạt động trong `S3StorageServiceImpl` và `TempUploadCleanupService`.

## 1. Tải ảnh lên (`uploadImage`)

Luồng này xử lý việc tải một tệp hình ảnh lên Amazon S3 và ghi nhận nó vào một bảng cơ sở dữ liệu tạm thời để ngăn chặn các tệp "mồ côi".

```mermaid
sequenceDiagram
    participant Client
    participant StorageService
    participant S3Client
    participant TempUploadRepository

    Client->>StorageService: uploadImage(MultipartFile, folder)
    activate StorageService

    StorageService->>StorageService: validateFile(file)
    alt trống, loại không hợp lệ hoặc quá lớn
        StorageService-->>Client: ném ra AppException
    end

    StorageService->>StorageService: buildS3Key(folder, originalFilename)
    
    StorageService->>StorageService: putObjectToS3(file, bucket, key)
    activate StorageService
    StorageService->>S3Client: putObject(PutObjectRequest)
    S3Client-->>StorageService: response
    deactivate StorageService

    StorageService->>StorageService: publicUrl = awsS3Properties.baseUrl + s3Key

    StorageService->>TempUploadRepository: save(TempUploadEntity(publicUrl))
    activate TempUploadRepository
    TempUploadRepository-->>StorageService: savedEntity
    deactivate TempUploadRepository

    StorageService-->>Client: UploadImageResponse(publicUrl)
    deactivate StorageService
```

## 2. Xóa tệp theo URL (`deleteByUrl`)

Luồng này xóa một tệp vật lý trực tiếp từ Amazon S3 bằng URL công khai của nó.

```mermaid
sequenceDiagram
    participant Client
    participant StorageService
    participant S3Client

    Client->>StorageService: deleteByUrl(publicUrl)
    activate StorageService

    StorageService->>StorageService: extractKeyFromUrl(publicUrl)
    
    StorageService->>S3Client: deleteObject(DeleteObjectRequest)
    activate S3Client
    S3Client-->>StorageService: response
    deactivate S3Client

    StorageService-->>Client: void
    deactivate StorageService
```

## 3. Cron Job dọn dẹp các tệp tải lên tạm thời (`cleanupOrphanUploads`)

Luồng này là một tác vụ chạy ngầm theo lịch (Cron Job). Nó quét bảng `temp_uploads` để tìm các hình ảnh đã được tải lên nhưng chưa bao giờ được liên kết với bất kỳ thực thể nghiệp vụ nào (ví dụ: Người dùng không nhấp vào "Lưu" sau khi tải lên) và xóa chúng để tiết kiệm không gian lưu trữ.

```mermaid
sequenceDiagram
    participant SpringScheduler
    participant TempUploadCleanupService
    participant TempUploadRepository
    participant StorageService
    participant Logger

    SpringScheduler->>TempUploadCleanupService: Kích hoạt @Scheduled(cron)
    activate TempUploadCleanupService

    TempUploadCleanupService->>TempUploadCleanupService: threshold = now() - CLEANUP_THRESHOLD_HOURS

    TempUploadCleanupService->>TempUploadRepository: findAllByCreatedAtBefore(threshold)
    activate TempUploadRepository
    TempUploadRepository-->>TempUploadCleanupService: List<TempUploadEntity> (tệp mồ côi)
    deactivate TempUploadRepository

    alt tệp mồ côi trống
        TempUploadCleanupService-->>SpringScheduler: trả về
    end

    loop Đối với mỗi tệp mồ côi
        alt Khối Try (Thành công)
            TempUploadCleanupService->>StorageService: deleteByUrl(orphan.url)
            TempUploadCleanupService->>TempUploadRepository: delete(orphan)
        else Bắt ngoại lệ
            TempUploadCleanupService->>Logger: log.error()
            Note right of Logger: Ngoại lệ bị bỏ qua,<br>tiếp tục với tệp mồ côi tiếp theo.
        end
    end

    TempUploadCleanupService-->>SpringScheduler: void
    deactivate TempUploadCleanupService
```
