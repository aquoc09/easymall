# Sequence Diagrams for Cart Service

This document contains the sequence diagrams for all operations within `CartServiceImpl`.

## 1. Get Cart (`getCart`)

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
    UserRepository-->>CartService: UserEntity (or throw USER_NOT_FOUND)
    deactivate UserRepository

    CartService->>CartRepository: findByUser_UserId(userId)
    activate CartRepository
    CartRepository-->>CartService: Optional<CartEntity>
    deactivate CartRepository

    alt Cart is present
        CartService->>CartService: buildCartResponse(cart)
        CartService-->>Client: CartResponse
    else Cart is empty
        CartService-->>Client: CartResponse (empty structure)
    end
    deactivate CartService
```

## 2. Add Item to Cart (`addItem`)

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
    alt Cart found
        CartRepository-->>CartService: CartEntity
    else Cart not found
        CartService->>CartRepository: save(new CartEntity)
        CartRepository-->>CartService: CartEntity
    end
    deactivate CartService

    CartService->>ProductVariantRepository: findById(request.variantId)
    ProductVariantRepository-->>CartService: ProductVariantEntity (or throw PRODUCT_VARIANT_NOT_FOUND)

    CartService->>CartService: validateNotBanned(variant)
    CartService->>CartService: calculateLimit(variant)

    CartService->>CartItemRepository: findByCart_CartIdAndVariant_VariantId()
    CartItemRepository-->>CartService: Optional<CartItemEntity>

    CartService->>CartService: calculate newTotalQty = currentQty + request.qty
    CartService->>CartService: validateQuantityLimit(variant, newTotalQty, limit)

    alt Item already exists in cart
        CartService->>CartItemRepository: save(updated item)
    else Item does not exist
        CartService->>CartItemRepository: save(new item)
    end

    CartService->>CartRepository: findByUser_UserId(userId) (reload cart)
    CartRepository-->>CartService: CartEntity
    
    CartService->>CartService: buildCartResponse(cart)
    CartService-->>Client: CartResponse
    deactivate CartService
```

## 3. Update Item in Cart (`updateItem`)

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
    alt Item not found
        CartItemRepository-->>CartService: Optional.empty()
        CartService-->>Client: throw AppException(CART_ITEM_NOT_FOUND)
    else Item found
        CartItemRepository-->>CartService: CartItemEntity
    end

    CartService->>CartService: validateNotBanned(variant)
    CartService->>CartService: calculateLimit(variant)
    CartService->>CartService: validateQuantityLimit(variant, request.qty, limit)

    CartService->>CartItemRepository: save(updated item)
    
    CartService->>CartRepository: findByUser_UserId(userId) (reload cart)
    CartRepository-->>CartService: CartEntity
    
    CartService->>CartService: buildCartResponse(cart)
    CartService-->>Client: CartResponse
    deactivate CartService
```

## 4. Remove Item from Cart (`removeItem`)

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
    alt Cart not found
        CartRepository-->>CartService: Optional.empty()
        CartService-->>Client: throw AppException(CART_NOT_FOUND)
    else Cart found
        CartRepository-->>CartService: CartEntity
    end

    CartService->>CartItemRepository: findByCart_CartIdAndVariant_VariantId()
    alt Item not found
        CartItemRepository-->>CartService: Optional.empty()
        CartService-->>Client: throw AppException(CART_ITEM_NOT_FOUND)
    else Item found
        CartItemRepository-->>CartService: CartItemEntity
    end

    CartService->>CartItemRepository: delete(item)
    
    CartService-->>Client: void
    deactivate CartService
```

## 5. Clear Cart (`clearCart`)

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
    alt Cart not found
        CartRepository-->>CartService: Optional.empty()
        CartService-->>Client: throw AppException(CART_NOT_FOUND)
    else Cart found
        CartRepository-->>CartService: CartEntity
    end

    CartService->>CartItemRepository: deleteByCart_CartId(cartId)
    
    CartService-->>Client: void
    deactivate CartService
```
