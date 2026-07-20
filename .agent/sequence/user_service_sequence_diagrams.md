# Sequence Diagrams for User Service

Tài liệu này chứa các sơ đồ tuần tự cho các hoạt động trong `UserServiceImpl`.

## 1. Lấy người dùng hiện tại (`getCurrentUser`)

Lấy hồ sơ của người dùng đã xác thực hiện tại dựa trên JWT token.

```mermaid
sequenceDiagram
    participant Client
    participant UserService
    participant SecurityContextHolder
    participant UserRepository

    Client->>UserService: getCurrentUser()
    activate UserService

    UserService->>SecurityContextHolder: getContext().getAuthentication().getName()
    activate SecurityContextHolder
    SecurityContextHolder-->>UserService: userEmail
    deactivate SecurityContextHolder

    UserService->>UserRepository: findByEmail(userEmail)
    activate UserRepository
    UserRepository-->>UserService: UserEntity (hoặc ném ra NOT_FOUND)
    deactivate UserRepository

    UserService->>UserService: toDetailResponse(user)
    
    UserService-->>Client: UserDetailResponse
    deactivate UserService
```

## 2. Tạo người dùng (`createUser`) - Dành cho Admin

```mermaid
sequenceDiagram
    participant Client
    participant UserService
    participant UserRepository
    participant RoleRepository
    participant PasswordEncoder

    Client->>UserService: createUser(CreateUserRequest)
    activate UserService

    UserService->>UserRepository: existsByEmail(request.email)
    activate UserRepository
    UserRepository-->>UserService: boolean
    deactivate UserRepository

    alt email đã tồn tại
        UserService-->>Client: throw AppException(EMAIL_ALREADY_EXISTS)
    end

    UserService->>RoleRepository: findById(request.roleId)
    activate RoleRepository
    RoleRepository-->>UserService: RoleEntity (hoặc ném ra NOT_FOUND)
    deactivate RoleRepository

    UserService->>PasswordEncoder: encode(request.password)
    activate PasswordEncoder
    PasswordEncoder-->>UserService: encodedPassword
    deactivate PasswordEncoder

    UserService->>UserService: Xây dựng UserEntity (isActive=true)

    UserService->>UserRepository: save(user)
    activate UserRepository
    UserRepository-->>UserService: savedUser
    deactivate UserRepository

    UserService->>UserService: toDetailResponse(savedUser)
    
    UserService-->>Client: UserDetailResponse
    deactivate UserService
```

## 3. Cập nhật người dùng (`updateUser`) - Dành cho Admin

```mermaid
sequenceDiagram
    participant Client
    participant UserService
    participant UserRepository
    participant RoleRepository

    Client->>UserService: updateUser(id, UpdateUserRequest)
    activate UserService

    UserService->>UserRepository: findById(id)
    activate UserRepository
    UserRepository-->>UserService: UserEntity (hoặc ném ra NOT_FOUND)
    deactivate UserRepository

    UserService->>UserService: Cập nhật các trường chung (Name, Phone, DOB, Avatar, isActive)

    alt request.roleId != null
        UserService->>RoleRepository: findById(request.roleId)
        activate RoleRepository
        RoleRepository-->>UserService: RoleEntity
        deactivate RoleRepository
        UserService->>UserService: user.setRole(role)
    end

    UserService->>UserRepository: save(user)
    activate UserRepository
    UserRepository-->>UserService: savedUser
    deactivate UserRepository

    UserService->>UserService: toDetailResponse(savedUser)
    
    UserService-->>Client: UserDetailResponse
    deactivate UserService
```

## 4. Xóa người dùng (`deleteUser`) - Xóa mềm (Soft Delete)

```mermaid
sequenceDiagram
    participant Client
    participant UserService
    participant UserRepository

    Client->>UserService: deleteUser(id)
    activate UserService

    UserService->>UserRepository: findById(id)
    activate UserRepository
    UserRepository-->>UserService: UserEntity (hoặc ném ra NOT_FOUND)
    deactivate UserRepository

    UserService->>UserService: user.setIsActive(false)

    UserService->>UserRepository: save(user)
    activate UserRepository
    UserRepository-->>UserService: savedUser
    deactivate UserRepository

    UserService-->>Client: void
    deactivate UserService
```

## 5. Đọc danh sách người dùng (`getAllUsers`, `getUserById`)

```mermaid
sequenceDiagram
    participant Client
    participant UserService
    participant UserRepository

    Client->>UserService: getAllUsers(pageable)
    activate UserService
    UserService->>UserRepository: findAll(pageable)
    activate UserRepository
    UserRepository-->>UserService: Page<UserEntity>
    deactivate UserRepository
    UserService->>UserService: toDetailResponse()
    UserService-->>Client: Page<UserDetailResponse>
    deactivate UserService

    Client->>UserService: getUserById(id)
    activate UserService
    UserService->>UserRepository: findById(id)
    activate UserRepository
    UserRepository-->>UserService: UserEntity
    deactivate UserRepository
    UserService->>UserService: toDetailResponse()
    UserService-->>Client: UserDetailResponse
    deactivate UserService
```
