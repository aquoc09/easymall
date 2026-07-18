# Sequence Diagrams for Tracking Service

This document contains the sequence diagrams for operations within `TrackingServiceImpl`.

## 1. Track Event (`trackEvent`)

This service records user behaviors (views, clicks, search, etc.) for analytics and recommendations. It operates asynchronously to prevent blocking the main application threads.

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
    Note over TrackingService: Method annotated with @Async.<br>Returns immediately to the caller.
    activate TrackingService

    TrackingService->>TrackingService: Build UserBehaviorEntity

    alt request.userId != null
        TrackingService->>UserRepository: getReferenceById(userId)
        UserRepository-->>TrackingService: User (Proxy reference)
    end

    alt request.productId != null
        TrackingService->>ProductRepository: getReferenceById(productId)
        ProductRepository-->>TrackingService: Product (Proxy reference)
    end

    alt request.categoryId != null
        TrackingService->>CategoryRepository: getReferenceById(categoryId)
        CategoryRepository-->>TrackingService: Category (Proxy reference)
    end

    alt Try Block (Success)
        TrackingService->>UserBehaviorRepository: save(UserBehaviorEntity)
        activate UserBehaviorRepository
        UserBehaviorRepository-->>TrackingService: savedEntity
        deactivate UserBehaviorRepository
    else Catch Exception
        TrackingService->>Logger: log.error("Failed to save tracking event...")
        Note right of Logger: Exception is swallowed <br>so the async thread doesn't crash
    end

    deactivate TrackingService
```
