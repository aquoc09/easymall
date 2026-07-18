# Sequence Diagrams for Contact Message Service

This document contains the sequence diagrams for all operations within `ContactMessageServiceImpl`.

## 1. Create Message (`createMessage`)

```mermaid
sequenceDiagram
    participant Client
    participant ContactMessageService
    participant UserRepository
    participant ContactMessageRepository
    participant ContactMessageMapper

    Client->>ContactMessageService: createMessage(request, userEmail)
    activate ContactMessageService

    alt userEmail is not empty (Logged in user)
        ContactMessageService->>UserRepository: findByEmail(userEmail)
        activate UserRepository
        UserRepository-->>ContactMessageService: UserEntity (or throw USER_NOT_FOUND)
        deactivate UserRepository
    else userEmail is empty (Guest user)
        alt guestName or guestEmail is missing
            ContactMessageService-->>Client: throw IllegalArgumentException
        end
    end

    ContactMessageService->>ContactMessageService: Build ContactMessageEntity (status="PENDING")

    ContactMessageService->>ContactMessageRepository: save(entity)
    activate ContactMessageRepository
    ContactMessageRepository-->>ContactMessageService: savedEntity
    deactivate ContactMessageRepository

    ContactMessageService->>ContactMessageMapper: toResponse(savedEntity)
    activate ContactMessageMapper
    ContactMessageMapper-->>ContactMessageService: ContactMessageResponse
    deactivate ContactMessageMapper

    ContactMessageService-->>Client: ContactMessageResponse
    deactivate ContactMessageService
```

## 2. Get My Messages (`getMyMessages`)

```mermaid
sequenceDiagram
    participant Client
    participant ContactMessageService
    participant UserRepository
    participant ContactMessageRepository
    participant ContactMessageMapper

    Client->>ContactMessageService: getMyMessages(userEmail, pageable)
    activate ContactMessageService

    ContactMessageService->>UserRepository: findByEmail(userEmail)
    activate UserRepository
    UserRepository-->>ContactMessageService: UserEntity (or throw USER_NOT_FOUND)
    deactivate UserRepository

    ContactMessageService->>ContactMessageRepository: findByUser_UserId(userId, pageable)
    activate ContactMessageRepository
    ContactMessageRepository-->>ContactMessageService: Page<ContactMessageEntity>
    deactivate ContactMessageRepository

    loop For each entity in Page
        ContactMessageService->>ContactMessageMapper: toResponse(entity)
        activate ContactMessageMapper
        ContactMessageMapper-->>ContactMessageService: ContactMessageResponse
        deactivate ContactMessageMapper
    end

    ContactMessageService-->>Client: Page<ContactMessageResponse>
    deactivate ContactMessageService
```

## 3. Get Admin Messages (`getAdminMessages`)

```mermaid
sequenceDiagram
    participant Client
    participant ContactMessageService
    participant ContactMessageRepository
    participant ContactMessageMapper

    Client->>ContactMessageService: getAdminMessages(status, pageable)
    activate ContactMessageService

    alt status is provided
        ContactMessageService->>ContactMessageRepository: findByStatus(status, pageable)
    else status is null or empty
        ContactMessageService->>ContactMessageRepository: findAll(pageable)
    end
    
    activate ContactMessageRepository
    ContactMessageRepository-->>ContactMessageService: Page<ContactMessageEntity>
    deactivate ContactMessageRepository

    loop For each entity in Page
        ContactMessageService->>ContactMessageMapper: toResponse(entity)
        activate ContactMessageMapper
        ContactMessageMapper-->>ContactMessageService: ContactMessageResponse
        deactivate ContactMessageMapper
    end

    ContactMessageService-->>Client: Page<ContactMessageResponse>
    deactivate ContactMessageService
```

## 4. Update Status (`updateStatus`)

```mermaid
sequenceDiagram
    participant Client
    participant ContactMessageService
    participant ContactMessageRepository
    participant ContactMessageMapper

    Client->>ContactMessageService: updateStatus(messageId, request)
    activate ContactMessageService

    ContactMessageService->>ContactMessageRepository: findById(messageId)
    activate ContactMessageRepository
    ContactMessageRepository-->>ContactMessageService: entity (or throw RESOURCE_NOT_FOUND)
    deactivate ContactMessageRepository

    alt entity.status != "PENDING"
        ContactMessageService-->>Client: throw AppException(INVALID_STATUS_TRANSITION)
    end

    ContactMessageService->>ContactMessageService: entity.setStatus(request.status)

    ContactMessageService->>ContactMessageRepository: save(entity)
    activate ContactMessageRepository
    ContactMessageRepository-->>ContactMessageService: savedEntity
    deactivate ContactMessageRepository

    ContactMessageService->>ContactMessageMapper: toResponse(savedEntity)
    activate ContactMessageMapper
    ContactMessageMapper-->>ContactMessageService: ContactMessageResponse
    deactivate ContactMessageMapper

    ContactMessageService-->>Client: ContactMessageResponse
    deactivate ContactMessageService
```
