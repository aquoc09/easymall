# Sequence Diagrams for Category Service

Tài liệu này chứa các sơ đồ tuần tự cho tất cả các hoạt động trong `CategoryServiceImpl`.

## 1. Lấy Cây Danh mục (`getCategoryTree`)

```mermaid
sequenceDiagram
    participant Client
    participant CategoryService
    participant CategoryRepository
    participant CategoryMapper

    Client->>CategoryService: getCategoryTree(isPublic)
    activate CategoryService
    
    alt isPublic == true
        CategoryService->>CategoryRepository: findByCategoryStatusOrderByLevelAscDisplayOrderAsc(1)
    else isPublic == false
        CategoryService->>CategoryRepository: findAllByOrderByLevelAscDisplayOrderAsc()
    end
    
    activate CategoryRepository
    CategoryRepository-->>CategoryService: List<CategoryEntity>
    deactivate CategoryRepository

    loop Đối với mỗi entity
        CategoryService->>CategoryMapper: toResponse(entity)
        activate CategoryMapper
        CategoryMapper-->>CategoryService: CategoryResponse
        deactivate CategoryMapper
    end
    
    CategoryService->>CategoryService: Xây dựng cấu trúc cây (gán children cho parent responses)

    CategoryService-->>Client: List<CategoryResponse> (các danh mục gốc)
    deactivate CategoryService
```

## 2. Tạo Danh mục (`createCategory`)

```mermaid
sequenceDiagram
    participant Client
    participant CategoryService
    participant CategoryRepository
    participant CategoryMapper

    Client->>CategoryService: createCategory(request)
    activate CategoryService

    CategoryService->>CategoryService: toSlug(request.categoryName)
    
    CategoryService->>CategoryRepository: existsByCategoryCode(categoryCode)
    activate CategoryRepository
    alt Code đã tồn tại
        CategoryRepository-->>CategoryService: true
        CategoryService-->>Client: throw AppException(CATEGORY_CODE_ALREADY_EXISTS)
    else Code khả dụng
        CategoryRepository-->>CategoryService: false
    end
    deactivate CategoryRepository

    CategoryService->>CategoryMapper: toEntity(request)
    CategoryMapper-->>CategoryService: CategoryEntity

    alt request.parentId != null
        CategoryService->>CategoryRepository: findById(request.parentId)
        activate CategoryRepository
        CategoryRepository-->>CategoryService: parentEntity (hoặc ném ra PARENT_CATEGORY_NOT_FOUND)
        deactivate CategoryRepository
        
        CategoryService->>CategoryService: tính toán newLevel = parent.level + 1
        alt newLevel > 3
            CategoryService-->>Client: throw AppException(MAX_LEVEL_EXCEEDED)
        end
        CategoryService->>CategoryService: entity.setLevel(newLevel)
    else request.parentId == null
        CategoryService->>CategoryService: entity.setLevel(1)
    end

    CategoryService->>CategoryService: entity.setCategoryStatus(1)

    CategoryService->>CategoryRepository: save(entity)
    activate CategoryRepository
    CategoryRepository-->>CategoryService: savedEntity
    deactivate CategoryRepository
    
    CategoryService->>CategoryMapper: toResponse(savedEntity)
    CategoryMapper-->>CategoryService: CategoryResponse

    CategoryService-->>Client: CategoryResponse
    deactivate CategoryService
```

## 3. Cập nhật Danh mục (`updateCategory`)

```mermaid
sequenceDiagram
    participant Client
    participant CategoryService
    participant CategoryRepository
    participant CategoryMapper

    Client->>CategoryService: updateCategory(categoryId, request)
    activate CategoryService

    CategoryService->>CategoryRepository: findById(categoryId)
    activate CategoryRepository
    CategoryRepository-->>CategoryService: entity (hoặc ném ra CATEGORY_NOT_FOUND)
    deactivate CategoryRepository
    
    CategoryService->>CategoryService: tính toán wasActive & isNowHidden
    CategoryService->>CategoryService: toSlug(request.categoryName)
    
    alt newCode != entity.code
        CategoryService->>CategoryRepository: existsByCategoryCode(newCode)
        activate CategoryRepository
        alt Code đã tồn tại
            CategoryRepository-->>CategoryService: true
            CategoryService-->>Client: throw AppException(CATEGORY_CODE_ALREADY_EXISTS)
        else Code khả dụng
            CategoryRepository-->>CategoryService: false
        end
        deactivate CategoryRepository
    end

    CategoryService->>CategoryMapper: updateEntityFromRequest(request, entity)
    CategoryService->>CategoryService: entity.setCategoryCode(newCode)

    CategoryService->>CategoryRepository: save(entity)
    activate CategoryRepository
    CategoryRepository-->>CategoryService: savedEntity
    deactivate CategoryRepository

    alt wasActive == true VÀ isNowHidden == true
        CategoryService->>CategoryService: cascadeHideChildren(categoryId)
        activate CategoryService
        CategoryService->>CategoryRepository: findAllByOrderByLevelAscDisplayOrderAsc()
        CategoryRepository-->>CategoryService: List<CategoryEntity>
        CategoryService->>CategoryService: gatherDescendantsToHide()
        CategoryService->>CategoryRepository: saveAll(toHide)
        deactivate CategoryService
    end

    CategoryService->>CategoryMapper: toResponse(savedEntity)
    CategoryMapper-->>CategoryService: CategoryResponse

    CategoryService-->>Client: CategoryResponse
    deactivate CategoryService
```

## 4. Xóa Danh mục (`deleteCategory`)

```mermaid
sequenceDiagram
    participant Client
    participant CategoryService
    participant CategoryRepository
    participant ProductRepository

    Client->>CategoryService: deleteCategory(categoryId)
    activate CategoryService

    CategoryService->>CategoryRepository: findById(categoryId)
    activate CategoryRepository
    CategoryRepository-->>CategoryService: entity (hoặc ném ra CATEGORY_NOT_FOUND)
    deactivate CategoryRepository

    CategoryService->>CategoryRepository: countByParentId(categoryId)
    activate CategoryRepository
    CategoryRepository-->>CategoryService: childrenCount
    deactivate CategoryRepository
    
    alt childrenCount > 0
        CategoryService-->>Client: throw AppException(CATEGORY_HAS_CHILDREN)
    end

    CategoryService->>ProductRepository: existsByCategoryId(categoryId)
    activate ProductRepository
    ProductRepository-->>CategoryService: hasProducts
    deactivate ProductRepository
    
    alt hasProducts == true
        CategoryService-->>Client: throw AppException(CATEGORY_IN_USE)
    end

    CategoryService->>CategoryRepository: delete(entity)
    activate CategoryRepository
    CategoryRepository-->>CategoryService: void
    deactivate CategoryRepository

    CategoryService-->>Client: void
    deactivate CategoryService
```
