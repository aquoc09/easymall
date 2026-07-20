# Sequence Diagrams for Review Service

Tài liệu này chứa các sơ đồ tuần tự cho các hoạt động trong `ReviewServiceImpl`.

## 1. Tạo Đánh giá (`createReview`)

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
    UserRepository-->>ReviewService: UserEntity (hoặc ném ra NOT_FOUND)
    deactivate UserRepository

    ReviewService->>ProductRepository: findById(productId)
    activate ProductRepository
    ProductRepository-->>ReviewService: ProductEntity (hoặc ném ra NOT_FOUND)
    deactivate ProductRepository

    ReviewService->>OrderRepository: findById(orderId)
    activate OrderRepository
    OrderRepository-->>ReviewService: OrderEntity (hoặc ném ra NOT_FOUND)
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

    alt request có imageUrls
        loop Đối với mỗi imageUrl
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

## 2. Lấy Đánh giá Sản phẩm (`getProductReviews`)

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

    ReviewService->>ReviewService: Ánh xạ thành Page<ReviewResponse>
    
    ReviewService-->>Client: Page<ReviewResponse>
    deactivate ReviewService
```

## 3. Lấy Tóm tắt Đánh giá Sản phẩm (`getProductReviewSummary`)

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

    ReviewService->>ReviewService: Khởi tạo breakdown map (1 đến 5 sao = 0)

    ReviewService->>ReviewRepository: countByRatingForProduct(productId)
    activate ReviewRepository
    ReviewRepository-->>ReviewService: List<Object[]> (Số lượng đánh giá)
    deactivate ReviewRepository

    ReviewService->>ReviewService: Điền kết quả vào breakdown map

    ReviewService->>ReviewService: Build ReviewSummaryResponse
    
    ReviewService-->>Client: ReviewSummaryResponse
    deactivate ReviewService
```

## 4. Lấy Đánh giá của tôi (`getMyReviews`)

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
    UserRepository-->>ReviewService: UserEntity (hoặc ném ra NOT_FOUND)
    deactivate UserRepository

    ReviewService->>ReviewRepository: findByUser_UserId(userId, pageable)
    activate ReviewRepository
    ReviewRepository-->>ReviewService: Page<ReviewEntity>
    deactivate ReviewRepository

    ReviewService->>ReviewService: Ánh xạ thành Page<ReviewResponse>
    
    ReviewService-->>Client: Page<ReviewResponse>
    deactivate ReviewService
```

## 5. Cập nhật Trạng thái Đánh giá - Admin (`updateReviewStatus`)

```mermaid
sequenceDiagram
    participant Client
    participant ReviewService
    participant ReviewRepository

    Client->>ReviewService: updateReviewStatus(reviewId, request)
    activate ReviewService

    ReviewService->>ReviewRepository: findById(reviewId)
    activate ReviewRepository
    ReviewRepository-->>ReviewService: ReviewEntity (hoặc ném ra NOT_FOUND)
    deactivate ReviewRepository

    ReviewService->>ReviewService: Đặt trạng thái = request.status

    ReviewService->>ReviewRepository: save(review)
    activate ReviewRepository
    ReviewRepository-->>ReviewService: savedReview
    deactivate ReviewRepository

    ReviewService->>ReviewService: toResponse(savedReview)
    
    ReviewService-->>Client: ReviewResponse
    deactivate ReviewService
```

## 6. Xóa Đánh giá (`deleteReview`)

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
    ReviewRepository-->>ReviewService: ReviewEntity (hoặc ném ra NOT_FOUND)
    deactivate ReviewRepository

    ReviewService->>UserRepository: findByEmail(userEmail)
    activate UserRepository
    UserRepository-->>ReviewService: UserEntity (hoặc ném ra NOT_FOUND)
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
