# Sequence Diagrams for Product Service

This document contains the sequence diagrams for operations within `ProductServiceImpl`.

## 1. Create Product (`createProduct`)

```mermaid
sequenceDiagram
    participant Client
    participant ProductService
    participant CategoryRepository
    participant ProductRepository
    participant ProductMapper

    Client->>ProductService: createProduct(ProductCreateRequest)
    activate ProductService

    alt request.categoryId != null
        ProductService->>CategoryRepository: existsById(categoryId)
        activate CategoryRepository
        CategoryRepository-->>ProductService: boolean
        deactivate CategoryRepository
        alt not exists
            ProductService-->>Client: throw AppException(CATEGORY_NOT_FOUND_FOR_PRODUCT)
        end
    end

    ProductService->>ProductMapper: toEntity(request)
    ProductService->>ProductService: ensureUniqueSlug(productName)

    ProductService->>ProductRepository: save(product)
    activate ProductRepository
    ProductRepository-->>ProductService: savedProduct
    deactivate ProductRepository

    ProductService->>ProductService: buildAndSaveVariants()
    ProductService->>ProductService: buildAndSaveImages()

    ProductService->>ProductRepository: findById(productId)
    activate ProductRepository
    ProductRepository-->>ProductService: fullProductEntity
    deactivate ProductRepository

    ProductService->>ProductMapper: toResponse(fullProductEntity)
    ProductMapper-->>ProductService: ProductResponse
    
    ProductService-->>Client: ProductResponse
    deactivate ProductService
```

## 2. Update Product (`updateProduct`)

```mermaid
sequenceDiagram
    participant Client
    participant ProductService
    participant CategoryRepository
    participant ProductRepository
    participant ProductMapper

    Client->>ProductService: updateProduct(productId, ProductUpdateRequest)
    activate ProductService

    ProductService->>ProductRepository: findById(productId)
    activate ProductRepository
    ProductRepository-->>ProductService: ProductEntity (or throw NOT_FOUND)
    deactivate ProductRepository

    alt request.categoryId != null
        ProductService->>CategoryRepository: existsById(categoryId)
        activate CategoryRepository
        CategoryRepository-->>ProductService: boolean
        deactivate CategoryRepository
        alt not exists
            ProductService-->>Client: throw AppException(CATEGORY_NOT_FOUND_FOR_PRODUCT)
        end
    end

    ProductService->>ProductMapper: updateEntityFromRequest()

    ProductService->>ProductService: buildAndSaveVariants() (Update, Create, Remove)
    
    alt request.images != null
        ProductService->>ProductService: product.getImages().clear()
        ProductService->>ProductRepository: save(product)
        ProductService->>ProductService: buildAndSaveImages()
    end

    ProductService->>ProductRepository: save(product)
    
    ProductService->>ProductRepository: findById(productId)
    activate ProductRepository
    ProductRepository-->>ProductService: fullProductEntity
    deactivate ProductRepository

    ProductService->>ProductMapper: toResponse(fullProductEntity)
    ProductMapper-->>ProductService: ProductResponse
    
    ProductService-->>Client: ProductResponse
    deactivate ProductService
```

## 3. Get Products (Admin & Public)

Applies to both `getAllProducts` (Admin) and `getPublicProducts` (Public).

```mermaid
sequenceDiagram
    participant Client
    participant ProductService
    participant CategoryRepository
    participant ProductRepository

    Client->>ProductService: getProducts(filter, pageable)
    activate ProductService

    ProductService->>ProductService: buildSpecification(filter, isPublic)
    activate ProductService
    
    alt isPublic == true
        ProductService->>ProductService: spec = isInStock(true)
    end
    
    alt filter has categoryCode
        ProductService->>CategoryRepository: findByCategoryCode(code)
        CategoryRepository-->>ProductService: CategoryEntity
        ProductService->>ProductService: collectAllDescendantCategoryIds (recursive)
        ProductService->>ProductService: spec.and(hasCategory(ids))
    end
    
    ProductService->>ProductService: Append other filters (Price, Keyword, Rating, etc.)
    deactivate ProductService

    ProductService->>ProductService: applyCollectionSorting(collection, pageable)

    ProductService->>ProductRepository: findAll(spec, pageable)
    activate ProductRepository
    ProductRepository-->>ProductService: Page<ProductEntity>
    deactivate ProductRepository

    ProductService->>ProductService: Map to Page<ProductResponse>
    
    ProductService-->>Client: Page<ProductResponse>
    deactivate ProductService
```

## 4. Delete Product (`deleteProduct`)

```mermaid
sequenceDiagram
    participant Client
    participant ProductService
    participant ProductRepository
    participant OrderDetailRepository
    participant CartItemRepository

    Client->>ProductService: deleteProduct(productId)
    activate ProductService

    ProductService->>ProductRepository: findById(productId)
    activate ProductRepository
    ProductRepository-->>ProductService: ProductEntity (or throw NOT_FOUND)
    deactivate ProductRepository

    ProductService->>ProductService: Get list of variantIds
    
    alt variantIds is not empty
        ProductService->>OrderDetailRepository: nullifyVariantReferences(variantIds)
        ProductService->>CartItemRepository: deleteByVariant_VariantIdIn(variantIds)
    end

    ProductService->>ProductRepository: delete(product)
    activate ProductRepository
    ProductRepository-->>ProductService: void
    deactivate ProductRepository

    ProductService-->>Client: void
    deactivate ProductService
```
