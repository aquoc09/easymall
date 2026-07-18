# Sequence Diagrams for Review Service

This document contains the sequence diagrams for operations within `ReviewServiceImpl`.

## 1. Create Review (`createReview`)

```mermaid
sequenceDiagram
    participant Client
    participant ReviewService
    participant UserRepository
    participant ProductRepository
    participant OrderRepository
    participant TempUploadRepository
    participant ReviewRepository

    Client->>ReviewService: createReview(CreateReviewRequest, userEmail)
    activate ReviewService

    ReviewService->>UserRepository: findByEmail(userEmail)
    activate UserRepository
    UserRepository-->>ReviewService: UserEntity (or throw NOT_FOUND)
    deactivate UserRepository

    ReviewService->>ProductRepository: findById(productId)
    activate ProductRepository
    ProductRepository-->>ReviewService: ProductEntity (or throw NOT_FOUND)
    deactivate ProductRepository

    ReviewService->>OrderRepository: findById(orderId)
    activate OrderRepository
    OrderRepository-->>ReviewService: OrderEntity (or throw NOT_FOUND)
    deactivate OrderRepository

    alt order.user.id != user.id
        ReviewService-->>Client: throw AppException(REVIEW_ORDER_OWNERSHIP_DENIED)
    end

    alt order.status != COMPLETED
        ReviewService-->>Client: throw AppException(REVIEW_ORDER_NOT_COMPLETED)
    end

    ReviewService->>ReviewRepository: existsByUser...AndProduct...AndOrder(...)
    activate ReviewRepository
    ReviewRepository-->>ReviewService: boolean
    deactivate ReviewRepository

    alt exists == true
        ReviewService-->>Client: throw AppException(REVIEW_ALREADY_EXISTS)
    end

    ReviewService->>ReviewService: Build ReviewEntity (Status: PENDING)

    alt request has imageUrls
        loop For each imageUrl
            ReviewService->>ReviewService: Build ReviewImageEntity
            ReviewService->>TempUploadRepository: deleteByUrl(url)
        end
    end

    ReviewService->>ReviewRepository: save(review)
    activate ReviewRepository
    ReviewRepository-->>ReviewService: savedReview
    deactivate ReviewRepository

    ReviewService->>ReviewService: toResponse(savedReview)
    
    ReviewService-->>Client: ReviewResponse
    deactivate ReviewService
```

## 2. Get Product Reviews (`getProductReviews`)

```mermaid
sequenceDiagram
    participant Client
    participant ReviewService
    participant ReviewRepository

    Client->>ReviewService: getProductReviews(productId, pageable)
    activate ReviewService

    ReviewService->>ReviewRepository: findByProduct_ProductIdAndReviewStatus(productId, APPROVED, pageable)
    activate ReviewRepository
    ReviewRepository-->>ReviewService: Page<ReviewEntity>
    deactivate ReviewRepository

    ReviewService->>ReviewService: Map to Page<ReviewResponse>
    
    ReviewService-->>Client: Page<ReviewResponse>
    deactivate ReviewService
```

## 3. Get Product Review Summary (`getProductReviewSummary`)

```mermaid
sequenceDiagram
    participant Client
    participant ReviewService
    participant ReviewRepository

    Client->>ReviewService: getProductReviewSummary(productId)
    activate ReviewService

    ReviewService->>ReviewRepository: countByProduct_ProductIdAndReviewStatus(productId, APPROVED)
    activate ReviewRepository
    ReviewRepository-->>ReviewService: long totalReviews
    deactivate ReviewRepository

    ReviewService->>ReviewRepository: findAverageRatingByProductId(productId)
    activate ReviewRepository
    ReviewRepository-->>ReviewService: Double averageRating
    deactivate ReviewRepository

    ReviewService->>ReviewService: Initialize breakdown map (1 to 5 stars = 0)

    ReviewService->>ReviewRepository: countByRatingForProduct(productId)
    activate ReviewRepository
    ReviewRepository-->>ReviewService: List<Object[]> (Rating counts)
    deactivate ReviewRepository

    ReviewService->>ReviewService: Populate breakdown map with results

    ReviewService->>ReviewService: Build ReviewSummaryResponse
    
    ReviewService-->>Client: ReviewSummaryResponse
    deactivate ReviewService
```

## 4. Get My Reviews (`getMyReviews`)

```mermaid
sequenceDiagram
    participant Client
    participant ReviewService
    participant UserRepository
    participant ReviewRepository

    Client->>ReviewService: getMyReviews(userEmail, pageable)
    activate ReviewService

    ReviewService->>UserRepository: findByEmail(userEmail)
    activate UserRepository
    UserRepository-->>ReviewService: UserEntity (or throw NOT_FOUND)
    deactivate UserRepository

    ReviewService->>ReviewRepository: findByUser_UserId(userId, pageable)
    activate ReviewRepository
    ReviewRepository-->>ReviewService: Page<ReviewEntity>
    deactivate ReviewRepository

    ReviewService->>ReviewService: Map to Page<ReviewResponse>
    
    ReviewService-->>Client: Page<ReviewResponse>
    deactivate ReviewService
```

## 5. Update Review Status - Admin (`updateReviewStatus`)

```mermaid
sequenceDiagram
    participant Client
    participant ReviewService
    participant ReviewRepository

    Client->>ReviewService: updateReviewStatus(reviewId, request)
    activate ReviewService

    ReviewService->>ReviewRepository: findById(reviewId)
    activate ReviewRepository
    ReviewRepository-->>ReviewService: ReviewEntity (or throw NOT_FOUND)
    deactivate ReviewRepository

    ReviewService->>ReviewService: Set status = request.status

    ReviewService->>ReviewRepository: save(review)
    activate ReviewRepository
    ReviewRepository-->>ReviewService: savedReview
    deactivate ReviewRepository

    ReviewService->>ReviewService: toResponse(savedReview)
    
    ReviewService-->>Client: ReviewResponse
    deactivate ReviewService
```

## 6. Delete Review (`deleteReview`)

```mermaid
sequenceDiagram
    participant Client
    participant ReviewService
    participant UserRepository
    participant ReviewRepository

    Client->>ReviewService: deleteReview(reviewId, userEmail)
    activate ReviewService

    ReviewService->>ReviewRepository: findById(reviewId)
    activate ReviewRepository
    ReviewRepository-->>ReviewService: ReviewEntity (or throw NOT_FOUND)
    deactivate ReviewRepository

    ReviewService->>UserRepository: findByEmail(userEmail)
    activate UserRepository
    UserRepository-->>ReviewService: UserEntity (or throw NOT_FOUND)
    deactivate UserRepository

    alt review.user.id != user.id
        ReviewService-->>Client: throw AppException(ACCESS_DENIED)
    end

    ReviewService->>ReviewRepository: delete(review)
    activate ReviewRepository
    ReviewRepository-->>ReviewService: void
    deactivate ReviewRepository

    ReviewService-->>Client: void
    deactivate ReviewService
```
