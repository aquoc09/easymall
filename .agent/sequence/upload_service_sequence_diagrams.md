# Sequence Diagrams for Upload Service

This document contains the sequence diagrams for operations within `S3StorageServiceImpl` and `TempUploadCleanupService`.

## 1. Upload Image (`uploadImage`)

This flow handles uploading an image file to Amazon S3 and recording it in a temporary database table to prevent "orphan" files.

```mermaid
sequenceDiagram
    participant Client
    participant StorageService
    participant S3Client
    participant TempUploadRepository

    Client->>StorageService: uploadImage(MultipartFile, folder)
    activate StorageService

    StorageService->>StorageService: validateFile(file)
    alt is empty, invalid type, or too large
        StorageService-->>Client: throw AppException
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

## 2. Delete File by URL (`deleteByUrl`)

This flow deletes a physical file directly from Amazon S3 using its public URL.

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

## 3. Temporary Upload Cleanup Cron Job (`cleanupOrphanUploads`)

This flow is a background scheduled task (Cron Job). It scans the `temp_uploads` table for images that were uploaded but never linked to any business entity (e.g. User didn't click "Save" after uploading) and deletes them to save storage space.

```mermaid
sequenceDiagram
    participant SpringScheduler
    participant TempUploadCleanupService
    participant TempUploadRepository
    participant StorageService
    participant Logger

    SpringScheduler->>TempUploadCleanupService: Trigger @Scheduled(cron)
    activate TempUploadCleanupService

    TempUploadCleanupService->>TempUploadCleanupService: threshold = now() - CLEANUP_THRESHOLD_HOURS

    TempUploadCleanupService->>TempUploadRepository: findAllByCreatedAtBefore(threshold)
    activate TempUploadRepository
    TempUploadRepository-->>TempUploadCleanupService: List<TempUploadEntity> (orphans)
    deactivate TempUploadRepository

    alt orphans is empty
        TempUploadCleanupService-->>SpringScheduler: return
    end

    loop For each orphan
        alt Try Block (Success)
            TempUploadCleanupService->>StorageService: deleteByUrl(orphan.url)
            TempUploadCleanupService->>TempUploadRepository: delete(orphan)
        else Catch Exception
            TempUploadCleanupService->>Logger: log.error()
            Note right of Logger: Exception is swallowed,<br>continues to next orphan.
        end
    end

    TempUploadCleanupService-->>SpringScheduler: void
    deactivate TempUploadCleanupService
```
