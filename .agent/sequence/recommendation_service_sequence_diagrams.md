# Sequence Diagrams for Recommendation Service

This document contains the sequence diagrams for operations within `RecommendationServiceImpl`.

## 1. Get Personalized Recommendations (`getPersonalizedRecommendations`)

```mermaid
sequenceDiagram
    participant Client
    participant RecommendationService
    participant UserRecommendationRepository
    participant ProductRepository
    participant ProductMapper

    Client->>RecommendationService: getPersonalizedRecommendations(userId, limit)
    activate RecommendationService

    alt userId is null
        RecommendationService->>RecommendationService: getFallbackTrendingProducts()
        activate RecommendationService
        RecommendationService->>ProductRepository: findTop10ByInStockTrueOrderBySoldCountDesc()
        activate ProductRepository
        ProductRepository-->>RecommendationService: List<ProductEntity> (Trending Products)
        deactivate ProductRepository
        deactivate RecommendationService
    else userId is not null
        RecommendationService->>UserRecommendationRepository: findTopRecommendationsForUser(userId, limit)
        activate UserRecommendationRepository
        UserRecommendationRepository-->>RecommendationService: List<UserRecommendationEntity>
        deactivate UserRecommendationRepository

        alt recommendations is empty
            RecommendationService->>RecommendationService: getFallbackTrendingProducts()
            activate RecommendationService
            RecommendationService->>ProductRepository: findTop10ByInStockTrueOrderBySoldCountDesc()
            activate ProductRepository
            ProductRepository-->>RecommendationService: List<ProductEntity> (Trending Products)
            deactivate ProductRepository
            deactivate RecommendationService
        else recommendations is not empty
            RecommendationService->>RecommendationService: Map to ProductEntity and filter (inStock == true)
        end
    end

    RecommendationService->>ProductMapper: toResponse()
    ProductMapper-->>RecommendationService: List<ProductResponse>

    RecommendationService-->>Client: List<ProductResponse>
    deactivate RecommendationService
```

## 2. Get Similar Products (`getSimilarProducts`)

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

    RecommendationService->>RecommendationService: Map to Similar ProductEntity and filter (inStock == true)

    RecommendationService->>ProductMapper: toResponse()
    ProductMapper-->>RecommendationService: List<ProductResponse>

    RecommendationService-->>Client: List<ProductResponse>
    deactivate RecommendationService
```

## 3. Get Bought Together Products (`getBoughtTogetherProducts`)

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

    RecommendationService->>RecommendationService: Map to Related ProductEntity and filter (inStock == true)

    RecommendationService->>ProductMapper: toResponse()
    ProductMapper-->>RecommendationService: List<ProductResponse>

    RecommendationService-->>Client: List<ProductResponse>
    deactivate RecommendationService
```
