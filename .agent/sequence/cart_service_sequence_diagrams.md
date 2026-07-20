# Sequence Diagrams for Cart Service

Tài liệu này chứa các sơ đồ tuần tự cho tất cả các hoạt động trong `CartServiceImpl`.

## 1. Lấy Giỏ hàng (`getCart`)

```mermaid
sequenceDiagram
    participant Client
    participant CartService
    participant UserRepository
    participant CartRepository

    Client->>CartService: getCart(email)
    activate CartService
    
    CartService->>UserRepository: findByEmail(email)
    activate UserRepository
    UserRepository-->>CartService: UserEntity (hoặc ném ra USER_NOT_FOUND)
    deactivate UserRepository

    CartService->>CartRepository: findByUser_UserId(userId)
    activate CartRepository
    CartRepository-->>CartService: Optional<CartEntity>
    deactivate CartRepository

    alt Giỏ hàng có tồn tại
        CartService->>CartService: buildCartResponse(cart)
        CartService-->>Client: CartResponse
    else Giỏ hàng trống
        CartService-->>Client: CartResponse (cấu trúc trống)
    end
    deactivate CartService
```

## 2. Thêm Sản phẩm vào Giỏ hàng (`addItem`)

```mermaid
sequenceDiagram
    participant Client
    participant CartService
    participant UserRepository
    participant CartRepository
    participant ProductVariantRepository
    participant CartItemRepository

    Client->>CartService: addItem(email, request)
    activate CartService

    CartService->>UserRepository: findByEmail(email)
    UserRepository-->>CartService: UserEntity

    CartService->>CartService: getOrCreateCart(user)
    activate CartService
    CartService->>CartRepository: findByUser_UserId(userId)
    alt Tìm thấy giỏ hàng
        CartRepository-->>CartService: CartEntity
    else Không tìm thấy giỏ hàng
        CartService->>CartRepository: save(new CartEntity)
        CartRepository-->>CartService: CartEntity
    end
    deactivate CartService

    CartService->>ProductVariantRepository: findById(request.variantId)
    ProductVariantRepository-->>CartService: ProductVariantEntity (hoặc ném ra PRODUCT_VARIANT_NOT_FOUND)

    CartService->>CartService: validateNotBanned(variant)
    CartService->>CartService: calculateLimit(variant)

    CartService->>CartItemRepository: findByCart_CartIdAndVariant_VariantId()
    CartItemRepository-->>CartService: Optional<CartItemEntity>

    CartService->>CartService: tính toán newTotalQty = currentQty + request.qty
    CartService->>CartService: validateQuantityLimit(variant, newTotalQty, limit)

    alt Sản phẩm đã tồn tại trong giỏ hàng
        CartService->>CartItemRepository: save(sản phẩm đã cập nhật)
    else Sản phẩm chưa tồn tại
        CartService->>CartItemRepository: save(sản phẩm mới)
    end

    CartService->>CartRepository: findByUser_UserId(userId) (tải lại giỏ hàng)
    CartRepository-->>CartService: CartEntity
    
    CartService->>CartService: buildCartResponse(cart)
    CartService-->>Client: CartResponse
    deactivate CartService
```

## 3. Cập nhật Sản phẩm trong Giỏ hàng (`updateItem`)

```mermaid
sequenceDiagram
    participant Client
    participant CartService
    participant UserRepository
    participant CartRepository
    participant CartItemRepository

    Client->>CartService: updateItem(email, variantId, request)
    activate CartService

    CartService->>UserRepository: findByEmail(email)
    UserRepository-->>CartService: UserEntity

    CartService->>CartService: getOrCreateCart(user)
    CartService-->>CartService: CartEntity

    CartService->>CartItemRepository: findByCart_CartIdAndVariant_VariantId()
    alt Không tìm thấy sản phẩm
        CartItemRepository-->>CartService: Optional.empty()
        CartService-->>Client: throw AppException(CART_ITEM_NOT_FOUND)
    else Tìm thấy sản phẩm
        CartItemRepository-->>CartService: CartItemEntity
    end

    CartService->>CartService: validateNotBanned(variant)
    CartService->>CartService: calculateLimit(variant)
    CartService->>CartService: validateQuantityLimit(variant, request.qty, limit)

    CartService->>CartItemRepository: save(sản phẩm đã cập nhật)
    
    CartService->>CartRepository: findByUser_UserId(userId) (tải lại giỏ hàng)
    CartRepository-->>CartService: CartEntity
    
    CartService->>CartService: buildCartResponse(cart)
    CartService-->>Client: CartResponse
    deactivate CartService
```

## 4. Xóa Sản phẩm khỏi Giỏ hàng (`removeItem`)

```mermaid
sequenceDiagram
    participant Client
    participant CartService
    participant UserRepository
    participant CartRepository
    participant CartItemRepository

    Client->>CartService: removeItem(email, variantId)
    activate CartService

    CartService->>UserRepository: findByEmail(email)
    UserRepository-->>CartService: UserEntity

    CartService->>CartRepository: findByUser_UserId(userId)
    alt Không tìm thấy giỏ hàng
        CartRepository-->>CartService: Optional.empty()
        CartService-->>Client: throw AppException(CART_NOT_FOUND)
    else Tìm thấy giỏ hàng
        CartRepository-->>CartService: CartEntity
    end

    CartService->>CartItemRepository: findByCart_CartIdAndVariant_VariantId()
    alt Không tìm thấy sản phẩm
        CartItemRepository-->>CartService: Optional.empty()
        CartService-->>Client: throw AppException(CART_ITEM_NOT_FOUND)
    else Tìm thấy sản phẩm
        CartItemRepository-->>CartService: CartItemEntity
    end

    CartService->>CartItemRepository: delete(item)
    
    CartService-->>Client: void
    deactivate CartService
```

## 5. Xóa sạch Giỏ hàng (`clearCart`)

```mermaid
sequenceDiagram
    participant Client
    participant CartService
    participant UserRepository
    participant CartRepository
    participant CartItemRepository

    Client->>CartService: clearCart(email)
    activate CartService

    CartService->>UserRepository: findByEmail(email)
    UserRepository-->>CartService: UserEntity

    CartService->>CartRepository: findByUser_UserId(userId)
    alt Không tìm thấy giỏ hàng
        CartRepository-->>CartService: Optional.empty()
        CartService-->>Client: throw AppException(CART_NOT_FOUND)
    else Tìm thấy giỏ hàng
        CartRepository-->>CartService: CartEntity
    end

    CartService->>CartItemRepository: deleteByCart_CartId(cartId)
    
    CartService-->>Client: void
    deactivate CartService
```
