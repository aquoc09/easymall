# Sequence Diagrams for User Service

This document contains the sequence diagrams for operations within `UserServiceImpl`.

## 1. Get Current User (`getCurrentUser`)

Retrieves the profile of the currently authenticated user based on the JWT token.

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
    UserRepository-->>UserService: UserEntity (or throw NOT_FOUND)
    deactivate UserRepository

    UserService->>UserService: toDetailResponse(user)
    
    UserService-->>Client: UserDetailResponse
    deactivate UserService
```

## 2. Create User (`createUser`) - Admin

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

    alt email already exists
        UserService-->>Client: throw AppException(EMAIL_ALREADY_EXISTS)
    end

    UserService->>RoleRepository: findById(request.roleId)
    activate RoleRepository
    RoleRepository-->>UserService: RoleEntity (or throw NOT_FOUND)
    deactivate RoleRepository

    UserService->>PasswordEncoder: encode(request.password)
    activate PasswordEncoder
    PasswordEncoder-->>UserService: encodedPassword
    deactivate PasswordEncoder

    UserService->>UserService: Build UserEntity (isActive=true)

    UserService->>UserRepository: save(user)
    activate UserRepository
    UserRepository-->>UserService: savedUser
    deactivate UserRepository

    UserService->>UserService: toDetailResponse(savedUser)
    
    UserService-->>Client: UserDetailResponse
    deactivate UserService
```

## 3. Update User (`updateUser`) - Admin

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
    UserRepository-->>UserService: UserEntity (or throw NOT_FOUND)
    deactivate UserRepository

    UserService->>UserService: Update generic fields (Name, Phone, DOB, Avatar, isActive)

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

## 4. Delete User (`deleteUser`) - Soft Delete

```mermaid
sequenceDiagram
    participant Client
    participant UserService
    participant UserRepository

    Client->>UserService: deleteUser(id)
    activate UserService

    UserService->>UserRepository: findById(id)
    activate UserRepository
    UserRepository-->>UserService: UserEntity (or throw NOT_FOUND)
    deactivate UserRepository

    UserService->>UserService: user.setIsActive(false)

    UserService->>UserRepository: save(user)
    activate UserRepository
    UserRepository-->>UserService: savedUser
    deactivate UserRepository

    UserService-->>Client: void
    deactivate UserService
```

## 5. Read Users (`getAllUsers`, `getUserById`)

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
