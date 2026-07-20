# Sequence Diagrams for Recommendation Service

Tài liệu này chứa các sơ đồ tuần tự cho các hoạt động trong `RecommendationServiceImpl`.

## 1. Lấy đề xuất được cá nhân hóa (`getPersonalizedRecommendations`)

```mermaid
sequenceDiagram
    participant Client
    participant RecommendationService
    participant UserRecommendationRepository
    participant ProductRepository
    participant ProductMapper

    Client->>RecommendationService: getPersonalizedRecommendations(userId, limit)
    activate RecommendationService

    alt userId là null
        RecommendationService->>RecommendationService: getFallbackTrendingProducts()
        activate RecommendationService
        RecommendationService->>ProductRepository: findTop10ByInStockTrueOrderBySoldCountDesc()
        activate ProductRepository
        ProductRepository-->>RecommendationService: List<ProductEntity> (Sản phẩm thịnh hành)
        deactivate ProductRepository
        deactivate RecommendationService
    else userId khác null
        RecommendationService->>UserRecommendationRepository: findTopRecommendationsForUser(userId, limit)
        activate UserRecommendationRepository
        UserRecommendationRepository-->>RecommendationService: List<UserRecommendationEntity>
        deactivate UserRecommendationRepository

        alt danh sách đề xuất trống
            RecommendationService->>RecommendationService: getFallbackTrendingProducts()
            activate RecommendationService
            RecommendationService->>ProductRepository: findTop10ByInStockTrueOrderBySoldCountDesc()
            activate ProductRepository
            ProductRepository-->>RecommendationService: List<ProductEntity> (Sản phẩm thịnh hành)
            deactivate ProductRepository
            deactivate RecommendationService
        else danh sách đề xuất không trống
            RecommendationService->>RecommendationService: Ánh xạ thành ProductEntity và lọc (inStock == true)
        end
    end

    RecommendationService->>ProductMapper: toResponse()
    ProductMapper-->>RecommendationService: List<ProductResponse>

    RecommendationService-->>Client: List<ProductResponse>
    deactivate RecommendationService
```

## 2. Lấy các sản phẩm tương tự (`getSimilarProducts`)

```mermaid
sequenceDiagram
    participant Client
    participant RecommendationService
    participant ProductSimilarityRepository
    participant ProductMapper

    Client->>RecommendationService: getSimilarProducts(productId, limit)
    activate RecommendationService

    RecommendationService->>ProductSimilarityRepository: findTopSimilarProducts(productId, limit)
    activate ProductSimilarityRepository
    ProductSimilarityRepository-->>RecommendationService: List<ProductSimilarityEntity>
    deactivate ProductSimilarityRepository

    RecommendationService->>RecommendationService: Ánh xạ thành Similar ProductEntity và lọc (inStock == true)

    RecommendationService->>ProductMapper: toResponse()
    ProductMapper-->>RecommendationService: List<ProductResponse>

    RecommendationService-->>Client: List<ProductResponse>
    deactivate RecommendationService
```

## 3. Lấy các sản phẩm được mua cùng nhau (`getBoughtTogetherProducts`)

```mermaid
sequenceDiagram
    participant Client
    participant RecommendationService
    participant ProductAssociationRepository
    participant ProductMapper

    Client->>RecommendationService: getBoughtTogetherProducts(productId, limit)
    activate RecommendationService

    RecommendationService->>ProductAssociationRepository: findTopBoughtTogetherProducts(productId, limit)
    activate ProductAssociationRepository
    ProductAssociationRepository-->>RecommendationService: List<ProductAssociationEntity>
    deactivate ProductAssociationRepository

    RecommendationService->>RecommendationService: Ánh xạ thành Related ProductEntity và lọc (inStock == true)

    RecommendationService->>ProductMapper: toResponse()
    ProductMapper-->>RecommendationService: List<ProductResponse>

    RecommendationService-->>Client: List<ProductResponse>
    deactivate RecommendationService
```
