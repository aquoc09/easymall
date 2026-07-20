# Sequence Diagrams for Tracking Service

Tài liệu này chứa các sơ đồ tuần tự cho các hoạt động trong `TrackingServiceImpl`.

## 1. Theo dõi sự kiện (`trackEvent`)

Dịch vụ này ghi lại các hành vi của người dùng (lượt xem, nhấp chuột, tìm kiếm, v.v.) để phân tích và đề xuất. Nó hoạt động bất đồng bộ để ngăn chặn việc chặn các luồng ứng dụng chính.

```mermaid
sequenceDiagram
    participant Client
    participant TrackingService
    participant UserRepository
    participant ProductRepository
    participant CategoryRepository
    participant UserBehaviorRepository
    participant Logger

    Client->>TrackingService: trackEvent(TrackingEventRequest)
    Note over TrackingService: Phương thức được chú thích với @Async.<br>Trả về ngay lập tức cho người gọi.
    activate TrackingService

    TrackingService->>TrackingService: Xây dựng UserBehaviorEntity

    alt request.userId != null
        TrackingService->>UserRepository: getReferenceById(userId)
        UserRepository-->>TrackingService: User (Tham chiếu proxy)
    end

    alt request.productId != null
        TrackingService->>ProductRepository: getReferenceById(productId)
        ProductRepository-->>TrackingService: Product (Tham chiếu proxy)
    end

    alt request.categoryId != null
        TrackingService->>CategoryRepository: getReferenceById(categoryId)
        CategoryRepository-->>TrackingService: Category (Tham chiếu proxy)
    end

    alt Khối Try (Thành công)
        TrackingService->>UserBehaviorRepository: save(UserBehaviorEntity)
        activate UserBehaviorRepository
        UserBehaviorRepository-->>TrackingService: savedEntity
        deactivate UserBehaviorRepository
    else Bắt ngoại lệ
        TrackingService->>Logger: log.error("Failed to save tracking event...")
        Note right of Logger: Ngoại lệ bị bỏ qua <br>để luồng bất đồng bộ không bị sập
    end

    deactivate TrackingService
```
