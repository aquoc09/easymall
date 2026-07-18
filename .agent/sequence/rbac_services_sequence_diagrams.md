# Sequence Diagrams for RBAC (Role Based Access Control) Services

This document contains the sequence diagrams for operations within `PermissionServiceImpl` and `RoleServiceImpl`.

## 1. Permission Service

### 1.1. Get All Permissions (`getAllPermissions`)

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

    PermissionService->>PermissionService: Map to List<PermissionResponse>
    
    PermissionService-->>Client: List<PermissionResponse>
    deactivate PermissionService
```

### 1.2. Create Permission (`createPermission`)

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

    alt exists is true
        PermissionService-->>Client: throw AppException(PERMISSION_ALREADY_EXISTS)
    end

    PermissionService->>PermissionService: Build PermissionEntity
    
    PermissionService->>PermissionRepository: save(entity)
    activate PermissionRepository
    PermissionRepository-->>PermissionService: savedEntity
    deactivate PermissionRepository

    PermissionService->>PermissionService: Map to PermissionResponse
    
    PermissionService-->>Client: PermissionResponse
    deactivate PermissionService
```

### 1.3. Update Permission (`updatePermission`)

```mermaid
sequenceDiagram
    participant Client
    participant PermissionService
    participant PermissionRepository

    Client->>PermissionService: updatePermission(id, UpdatePermissionRequest)
    activate PermissionService

    PermissionService->>PermissionRepository: findById(id)
    activate PermissionRepository
    PermissionRepository-->>PermissionService: PermissionEntity (or throw NOT_FOUND)
    deactivate PermissionRepository

    PermissionService->>PermissionService: Update description
    
    PermissionService->>PermissionRepository: save(entity)
    activate PermissionRepository
    PermissionRepository-->>PermissionService: savedEntity
    deactivate PermissionRepository

    PermissionService->>PermissionService: Map to PermissionResponse
    
    PermissionService-->>Client: PermissionResponse
    deactivate PermissionService
```

### 1.4. Delete Permission (`deletePermission`)

```mermaid
sequenceDiagram
    participant Client
    participant PermissionService
    participant PermissionRepository

    Client->>PermissionService: deletePermission(id)
    activate PermissionService

    PermissionService->>PermissionRepository: findById(id)
    activate PermissionRepository
    PermissionRepository-->>PermissionService: PermissionEntity (or throw NOT_FOUND)
    deactivate PermissionRepository

    PermissionService->>PermissionRepository: delete(entity)
    activate PermissionRepository
    PermissionRepository-->>PermissionService: void
    deactivate PermissionRepository

    PermissionService-->>Client: void
    deactivate PermissionService
```

---

## 2. Role Service

### 2.1. Get All Roles (`getAllRoles`)

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

    loop For each RoleEntity
        RoleService->>RoleRepository: findByIdWithPermissions(roleId)
        RoleRepository-->>RoleService: RoleEntity (with permissions loaded)
        RoleService->>RoleService: Map to RoleResponse
    end
    
    RoleService-->>Client: List<RoleResponse>
    deactivate RoleService
```

### 2.2. Get Role By ID (`getRoleById`)

```mermaid
sequenceDiagram
    participant Client
    participant RoleService
    participant RoleRepository

    Client->>RoleService: getRoleById(id)
    activate RoleService

    RoleService->>RoleRepository: findByIdWithPermissions(id)
    activate RoleRepository
    RoleRepository-->>RoleService: RoleEntity (or throw ROLE_NOT_FOUND)
    deactivate RoleRepository

    RoleService->>RoleService: Map to RoleResponse
    
    RoleService-->>Client: RoleResponse
    deactivate RoleService
```

### 2.3. Create Role (`createRole`)

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

    alt exists is true
        RoleService-->>Client: throw AppException(ROLE_ALREADY_EXISTS)
    end

    RoleService->>PermissionRepository: findAllByPermissionIdIn(request.permissionIds)
    activate PermissionRepository
    PermissionRepository-->>RoleService: Set<PermissionEntity>
    deactivate PermissionRepository

    alt sizes do not match
        RoleService-->>Client: throw AppException(PERMISSION_NOT_FOUND)
    end

    RoleService->>RoleService: Build RoleEntity with permissions
    
    RoleService->>RoleRepository: save(entity)
    activate RoleRepository
    RoleRepository-->>RoleService: savedEntity
    deactivate RoleRepository

    RoleService->>RoleService: Map to RoleResponse
    
    RoleService-->>Client: RoleResponse
    deactivate RoleService
```

### 2.4. Update Role (`updateRole`)

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
    RoleRepository-->>RoleService: RoleEntity (or throw ROLE_NOT_FOUND)
    deactivate RoleRepository

    RoleService->>PermissionRepository: findAllByPermissionIdIn(request.permissionIds)
    activate PermissionRepository
    PermissionRepository-->>RoleService: Set<PermissionEntity>
    deactivate PermissionRepository

    alt sizes do not match
        RoleService-->>Client: throw AppException(PERMISSION_NOT_FOUND)
    end

    RoleService->>RoleService: Update description and permissions
    
    RoleService->>RoleRepository: save(entity)
    activate RoleRepository
    RoleRepository-->>RoleService: savedEntity
    deactivate RoleRepository

    RoleService->>RoleService: Map to RoleResponse
    
    RoleService-->>Client: RoleResponse
    deactivate RoleService
```

### 2.5. Delete Role (`deleteRole`)

```mermaid
sequenceDiagram
    participant Client
    participant RoleService
    participant RoleRepository

    Client->>RoleService: deleteRole(id)
    activate RoleService

    RoleService->>RoleRepository: findById(id)
    activate RoleRepository
    RoleRepository-->>RoleService: RoleEntity (or throw ROLE_NOT_FOUND)
    deactivate RoleRepository

    alt roleName is PROTECTED (ROLE_ADMIN or ROLE_USER)
        RoleService-->>Client: throw AppException(ROLE_PROTECTED)
    end

    RoleService->>RoleRepository: delete(entity)
    activate RoleRepository
    RoleRepository-->>RoleService: void
    deactivate RoleRepository

    RoleService-->>Client: void
    deactivate RoleService
```
