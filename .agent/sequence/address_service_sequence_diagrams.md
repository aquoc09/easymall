# Sequence Diagrams for Address Service

This document contains the sequence diagrams for all operations within `AddressServiceImpl`.

## 1. Get My Addresses (`getMyAddresses`)

```mermaid
sequenceDiagram
    participant Client
    participant AddressService
    participant UserRepository
    participant AddressRepository
    participant GhnMasterDataService

    Client->>AddressService: getMyAddresses(userEmail)
    activate AddressService
    
    AddressService->>UserRepository: findByEmail(userEmail)
    activate UserRepository
    alt User not found
        UserRepository-->>AddressService: Optional.empty()
        AddressService-->>Client: throw AppException(USER_NOT_FOUND)
    else User found
        UserRepository-->>AddressService: Optional<UserEntity>
    end
    deactivate UserRepository

    AddressService->>AddressRepository: findAllByUserUserId(userId)
    activate AddressRepository
    AddressRepository-->>AddressService: List<AddressEntity>
    deactivate AddressRepository

    loop For each AddressEntity
        AddressService->>AddressService: toResponse(entity)
        activate AddressService
        AddressService->>GhnMasterDataService: getProvinces()
        GhnMasterDataService-->>AddressService: List<GhnProvinceResponse>
        AddressService->>GhnMasterDataService: getDistricts(provinceId)
        GhnMasterDataService-->>AddressService: List<GhnDistrictResponse>
        AddressService->>GhnMasterDataService: getWards(districtId)
        GhnMasterDataService-->>AddressService: List<GhnWardResponse>
        AddressService-->>AddressService: AddressResponse
        deactivate AddressService
    end

    AddressService-->>Client: List<AddressResponse>
    deactivate AddressService
```

## 2. Create Address (`createAddress`)

```mermaid
sequenceDiagram
    participant Client
    participant AddressService
    participant UserRepository
    participant AddressRepository
    participant GhnMasterDataService

    Client->>AddressService: createAddress(request, userEmail)
    activate AddressService
    
    AddressService->>UserRepository: findByEmail(userEmail)
    activate UserRepository
    UserRepository-->>AddressService: Optional<UserEntity>
    deactivate UserRepository

    AddressService->>AddressRepository: countByUserUserId(userId)
    activate AddressRepository
    AddressRepository-->>AddressService: count
    deactivate AddressRepository
    
    alt count >= MAX_ADDRESSES (5)
        AddressService-->>Client: throw AppException(ADDRESS_MAX_LIMIT)
    end

    AddressService->>GhnMasterDataService: getProvinces(), getDistricts(), getWards()
    activate GhnMasterDataService
    GhnMasterDataService-->>AddressService: names
    deactivate GhnMasterDataService
    
    AddressService->>AddressService: buildFullAddress()

    alt request.isDefault == true
        AddressService->>AddressService: unsetAllDefaults(userId)
        activate AddressService
        AddressService->>AddressRepository: findAllByUserUserId(userId)
        AddressRepository-->>AddressService: List<AddressEntity>
        AddressService->>AddressRepository: saveAll(addresses)
        deactivate AddressService
    end

    alt count == 0
        AddressService->>AddressService: entity.setIsDefault(true)
    end

    AddressService->>AddressRepository: save(entity)
    activate AddressRepository
    AddressRepository-->>AddressService: savedEntity
    deactivate AddressRepository

    AddressService->>AddressService: toResponse(savedEntity)
    
    AddressService-->>Client: AddressResponse
    deactivate AddressService
```

## 3. Update Address (`updateAddress`)

```mermaid
sequenceDiagram
    participant Client
    participant AddressService
    participant UserRepository
    participant AddressRepository
    participant GhnMasterDataService

    Client->>AddressService: updateAddress(addressId, request, userEmail)
    activate AddressService
    
    AddressService->>AddressService: findAddressAndValidateOwnership()
    activate AddressService
    AddressService->>UserRepository: findByEmail(userEmail)
    UserRepository-->>AddressService: UserEntity
    AddressService->>AddressRepository: findByAddressIdAndUser_UserId(addressId, userId)
    AddressRepository-->>AddressService: AddressEntity
    deactivate AddressService

    AddressService->>AddressService: update fields from request

    AddressService->>GhnMasterDataService: getProvinces(), getDistricts(), getWards()
    GhnMasterDataService-->>AddressService: names
    
    AddressService->>AddressService: buildFullAddress()

    alt request.isDefault == true
        AddressService->>AddressService: unsetAllDefaults(userId)
        AddressService->>AddressService: entity.setIsDefault(true)
    end

    AddressService->>AddressRepository: save(entity)
    activate AddressRepository
    AddressRepository-->>AddressService: savedEntity
    deactivate AddressRepository

    AddressService->>AddressService: toResponse(savedEntity)
    
    AddressService-->>Client: AddressResponse
    deactivate AddressService
```

## 4. Delete Address (`deleteAddress`)

```mermaid
sequenceDiagram
    participant Client
    participant AddressService
    participant UserRepository
    participant AddressRepository

    Client->>AddressService: deleteAddress(addressId, userEmail)
    activate AddressService
    
    AddressService->>AddressService: findAddressAndValidateOwnership()
    activate AddressService
    AddressService->>UserRepository: findByEmail(userEmail)
    UserRepository-->>AddressService: UserEntity
    AddressService->>AddressRepository: findByAddressIdAndUser_UserId(addressId, userId)
    AddressRepository-->>AddressService: AddressEntity
    deactivate AddressService

    AddressService->>AddressRepository: delete(entity)
    activate AddressRepository
    AddressRepository-->>AddressService: void
    deactivate AddressRepository

    AddressService-->>Client: void
    deactivate AddressService
```

## 5. Set Default (`setDefault`)

```mermaid
sequenceDiagram
    participant Client
    participant AddressService
    participant UserRepository
    participant AddressRepository

    Client->>AddressService: setDefault(addressId, userEmail)
    activate AddressService
    
    AddressService->>AddressService: findAddressAndValidateOwnership()
    activate AddressService
    AddressService->>UserRepository: findByEmail(userEmail)
    UserRepository-->>AddressService: UserEntity
    AddressService->>AddressRepository: findByAddressIdAndUser_UserId(addressId, userId)
    AddressRepository-->>AddressService: AddressEntity
    deactivate AddressService

    AddressService->>AddressService: unsetAllDefaults(userId)
    activate AddressService
    AddressService->>AddressRepository: findAllByUserUserId(userId)
    AddressRepository-->>AddressService: List<AddressEntity>
    AddressService->>AddressRepository: saveAll(addresses)
    deactivate AddressService

    AddressService->>AddressService: entity.setIsDefault(true)

    AddressService->>AddressRepository: save(entity)
    activate AddressRepository
    AddressRepository-->>AddressService: savedEntity
    deactivate AddressRepository
    
    AddressService->>AddressService: toResponse(savedEntity)

    AddressService-->>Client: AddressResponse
    deactivate AddressService
```
