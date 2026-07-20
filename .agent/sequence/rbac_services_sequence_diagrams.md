# Sequence Diagrams for RBAC (Role Based Access Control) Services

Tài liệu này chứa các sơ đồ tuần tự cho các hoạt động trong `PermissionServiceImpl` và `RoleServiceImpl`.

## 1. Dịch vụ Quyền (Permission Service)

### 1.1. Lấy Tất cả các Quyền (`getAllPermissions`)

```mermaid
sequenceDiagram
    participant Client
    participant PermissionService
    participant PermissionRepository

    Client->>PermissionService: getAllPermissions()
    activate PermissionService

    PermissionService->>PermissionRepository: findAll()
    activate PermissionRepository
    PermissionRepository-->>PermissionService: List<PermissionEntity>
    deactivate PermissionRepository

    PermissionService->>PermissionService: Ánh xạ thành List<PermissionResponse>
    
    PermissionService-->>Client: List<PermissionResponse>
    deactivate PermissionService
```

### 1.2. Tạo Quyền (`createPermission`)

```mermaid
sequenceDiagram
    participant Client
    participant PermissionService
    participant PermissionRepository

    Client->>PermissionService: createPermission(CreatePermissionRequest)
    activate PermissionService

    PermissionService->>PermissionRepository: existsByPermissionName(request.permissionName)
    activate PermissionRepository
    PermissionRepository-->>PermissionService: boolean
    deactivate PermissionRepository

    alt exists là true
        PermissionService-->>Client: throw AppException(PERMISSION_ALREADY_EXISTS)
    end

    PermissionService->>PermissionService: Xây dựng PermissionEntity
    
    PermissionService->>PermissionRepository: save(entity)
    activate PermissionRepository
    PermissionRepository-->>PermissionService: savedEntity
    deactivate PermissionRepository

    PermissionService->>PermissionService: Ánh xạ thành PermissionResponse
    
    PermissionService-->>Client: PermissionResponse
    deactivate PermissionService
```

### 1.3. Cập nhật Quyền (`updatePermission`)

```mermaid
sequenceDiagram
    participant Client
    participant PermissionService
    participant PermissionRepository

    Client->>PermissionService: updatePermission(id, UpdatePermissionRequest)
    activate PermissionService

    PermissionService->>PermissionRepository: findById(id)
    activate PermissionRepository
    PermissionRepository-->>PermissionService: PermissionEntity (hoặc ném ra NOT_FOUND)
    deactivate PermissionRepository

    PermissionService->>PermissionService: Cập nhật description
    
    PermissionService->>PermissionRepository: save(entity)
    activate PermissionRepository
    PermissionRepository-->>PermissionService: savedEntity
    deactivate PermissionRepository

    PermissionService->>PermissionService: Ánh xạ thành PermissionResponse
    
    PermissionService-->>Client: PermissionResponse
    deactivate PermissionService
```

### 1.4. Xóa Quyền (`deletePermission`)

```mermaid
sequenceDiagram
    participant Client
    participant PermissionService
    participant PermissionRepository

    Client->>PermissionService: deletePermission(id)
    activate PermissionService

    PermissionService->>PermissionRepository: findById(id)
    activate PermissionRepository
    PermissionRepository-->>PermissionService: PermissionEntity (hoặc ném ra NOT_FOUND)
    deactivate PermissionRepository

    PermissionService->>PermissionRepository: delete(entity)
    activate PermissionRepository
    PermissionRepository-->>PermissionService: void
    deactivate PermissionRepository

    PermissionService-->>Client: void
    deactivate PermissionService
```

---

## 2. Dịch vụ Vai trò (Role Service)

### 2.1. Lấy Tất cả Vai trò (`getAllRoles`)

```mermaid
sequenceDiagram
    participant Client
    participant RoleService
    participant RoleRepository

    Client->>RoleService: getAllRoles()
    activate RoleService

    RoleService->>RoleRepository: findAll()
    activate RoleRepository
    RoleRepository-->>RoleService: List<RoleEntity>
    deactivate RoleRepository

    loop Đối với mỗi RoleEntity
        RoleService->>RoleRepository: findByIdWithPermissions(roleId)
        RoleRepository-->>RoleService: RoleEntity (với các quyền đã được tải)
        RoleService->>RoleService: Ánh xạ thành RoleResponse
    end
    
    RoleService-->>Client: List<RoleResponse>
    deactivate RoleService
```

### 2.2. Lấy Vai trò theo ID (`getRoleById`)

```mermaid
sequenceDiagram
    participant Client
    participant RoleService
    participant RoleRepository

    Client->>RoleService: getRoleById(id)
    activate RoleService

    RoleService->>RoleRepository: findByIdWithPermissions(id)
    activate RoleRepository
    RoleRepository-->>RoleService: RoleEntity (hoặc ném ra ROLE_NOT_FOUND)
    deactivate RoleRepository

    RoleService->>RoleService: Ánh xạ thành RoleResponse
    
    RoleService-->>Client: RoleResponse
    deactivate RoleService
```

### 2.3. Tạo Vai trò (`createRole`)

```mermaid
sequenceDiagram
    participant Client
    participant RoleService
    participant RoleRepository
    participant PermissionRepository

    Client->>RoleService: createRole(CreateRoleRequest)
    activate RoleService

    RoleService->>RoleRepository: existsByRoleName(request.roleName)
    activate RoleRepository
    RoleRepository-->>RoleService: boolean
    deactivate RoleRepository

    alt exists là true
        RoleService-->>Client: throw AppException(ROLE_ALREADY_EXISTS)
    end

    RoleService->>PermissionRepository: findAllByPermissionIdIn(request.permissionIds)
    activate PermissionRepository
    PermissionRepository-->>RoleService: Set<PermissionEntity>
    deactivate PermissionRepository

    alt kích thước không khớp
        RoleService-->>Client: throw AppException(PERMISSION_NOT_FOUND)
    end

    RoleService->>RoleService: Xây dựng RoleEntity với các quyền
    
    RoleService->>RoleRepository: save(entity)
    activate RoleRepository
    RoleRepository-->>RoleService: savedEntity
    deactivate RoleRepository

    RoleService->>RoleService: Ánh xạ thành RoleResponse
    
    RoleService-->>Client: RoleResponse
    deactivate RoleService
```

### 2.4. Cập nhật Vai trò (`updateRole`)

```mermaid
sequenceDiagram
    participant Client
    participant RoleService
    participant RoleRepository
    participant PermissionRepository

    Client->>RoleService: updateRole(id, UpdateRoleRequest)
    activate RoleService

    RoleService->>RoleRepository: findByIdWithPermissions(id)
    activate RoleRepository
    RoleRepository-->>RoleService: RoleEntity (hoặc ném ra ROLE_NOT_FOUND)
    deactivate RoleRepository

    RoleService->>PermissionRepository: findAllByPermissionIdIn(request.permissionIds)
    activate PermissionRepository
    PermissionRepository-->>RoleService: Set<PermissionEntity>
    deactivate PermissionRepository

    alt kích thước không khớp
        RoleService-->>Client: throw AppException(PERMISSION_NOT_FOUND)
    end

    RoleService->>RoleService: Cập nhật description và permissions
    
    RoleService->>RoleRepository: save(entity)
    activate RoleRepository
    RoleRepository-->>RoleService: savedEntity
    deactivate RoleRepository

    RoleService->>RoleService: Ánh xạ thành RoleResponse
    
    RoleService-->>Client: RoleResponse
    deactivate RoleService
```

### 2.5. Xóa Vai trò (`deleteRole`)

```mermaid
sequenceDiagram
    participant Client
    participant RoleService
    participant RoleRepository

    Client->>RoleService: deleteRole(id)
    activate RoleService

    RoleService->>RoleRepository: findById(id)
    activate RoleRepository
    RoleRepository-->>RoleService: RoleEntity (hoặc ném ra ROLE_NOT_FOUND)
    deactivate RoleRepository

    alt roleName được BẢO VỆ (ROLE_ADMIN hoặc ROLE_USER)
        RoleService-->>Client: throw AppException(ROLE_PROTECTED)
    end

    RoleService->>RoleRepository: delete(entity)
    activate RoleRepository
    RoleRepository-->>RoleService: void
    deactivate RoleRepository

    RoleService-->>Client: void
    deactivate RoleService
```
