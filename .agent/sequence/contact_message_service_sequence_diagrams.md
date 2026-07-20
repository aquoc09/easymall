# Sequence Diagrams for Contact Message Service

Tài liệu này chứa các sơ đồ tuần tự cho tất cả các hoạt động trong `ContactMessageServiceImpl`.

## 1. Tạo tin nhắn (`createMessage`)

```mermaid
sequenceDiagram
    participant Client
    participant ContactMessageService
    participant UserRepository
    participant ContactMessageRepository
    participant ContactMessageMapper

    Client->>ContactMessageService: createMessage(request, userEmail)
    activate ContactMessageService

    alt userEmail không rỗng (Người dùng đã đăng nhập)
        ContactMessageService->>UserRepository: findByEmail(userEmail)
        activate UserRepository
        UserRepository-->>ContactMessageService: UserEntity (hoặc ném ra USER_NOT_FOUND)
        deactivate UserRepository
    else userEmail rỗng (Người dùng khách)
        alt thiếu guestName hoặc guestEmail
            ContactMessageService-->>Client: ném ra IllegalArgumentException
        end
    end

    ContactMessageService->>ContactMessageService: Xây dựng ContactMessageEntity (status="PENDING")

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

## 2. Lấy tin nhắn của tôi (`getMyMessages`)

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
    UserRepository-->>ContactMessageService: UserEntity (hoặc ném ra USER_NOT_FOUND)
    deactivate UserRepository

    ContactMessageService->>ContactMessageRepository: findByUser_UserId(userId, pageable)
    activate ContactMessageRepository
    ContactMessageRepository-->>ContactMessageService: Page<ContactMessageEntity>
    deactivate ContactMessageRepository

    loop Đối với mỗi entity trong Page
        ContactMessageService->>ContactMessageMapper: toResponse(entity)
        activate ContactMessageMapper
        ContactMessageMapper-->>ContactMessageService: ContactMessageResponse
        deactivate ContactMessageMapper
    end

    ContactMessageService-->>Client: Page<ContactMessageResponse>
    deactivate ContactMessageService
```

## 3. Lấy tin nhắn của Admin (`getAdminMessages`)

```mermaid
sequenceDiagram
    participant Client
    participant ContactMessageService
    participant ContactMessageRepository
    participant ContactMessageMapper

    Client->>ContactMessageService: getAdminMessages(status, pageable)
    activate ContactMessageService

    alt status được cung cấp
        ContactMessageService->>ContactMessageRepository: findByStatus(status, pageable)
    else status là null hoặc rỗng
        ContactMessageService->>ContactMessageRepository: findAll(pageable)
    end
    
    activate ContactMessageRepository
    ContactMessageRepository-->>ContactMessageService: Page<ContactMessageEntity>
    deactivate ContactMessageRepository

    loop Đối với mỗi entity trong Page
        ContactMessageService->>ContactMessageMapper: toResponse(entity)
        activate ContactMessageMapper
        ContactMessageMapper-->>ContactMessageService: ContactMessageResponse
        deactivate ContactMessageMapper
    end

    ContactMessageService-->>Client: Page<ContactMessageResponse>
    deactivate ContactMessageService
```

## 4. Cập nhật trạng thái (`updateStatus`)

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
    ContactMessageRepository-->>ContactMessageService: entity (hoặc ném ra RESOURCE_NOT_FOUND)
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
