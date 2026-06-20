package com.quocnva.easymall.service.role.impl;

import com.quocnva.easymall.dtos.request.role.CreateRoleRequest;
import com.quocnva.easymall.dtos.request.role.UpdateRoleRequest;
import com.quocnva.easymall.dtos.response.permission.PermissionResponse;
import com.quocnva.easymall.dtos.response.role.RoleResponse;
import com.quocnva.easymall.entity.PermissionEntity;
import com.quocnva.easymall.entity.RoleEntity;
import com.quocnva.easymall.exception.AppException;
import com.quocnva.easymall.exception.ErrorCode;
import com.quocnva.easymall.repository.PermissionRepository;
import com.quocnva.easymall.repository.RoleRepository;
import com.quocnva.easymall.service.role.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private static final Set<String> PROTECTED_ROLES = Set.of("ROLE_ADMIN", "ROLE_USER");

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    @Override
    @Transactional(readOnly = true)
    public List<RoleResponse> getAllRoles() {
        return roleRepository.findAll().stream()
                .map(role -> {
                    // Eager fetch permissions for each role
                    RoleEntity loaded = roleRepository.findByIdWithPermissions(role.getRoleId())
                            .orElse(role);
                    return toResponse(loaded);
                })
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public RoleResponse getRoleById(Long id) {
        RoleEntity role = roleRepository.findByIdWithPermissions(id)
                .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));
        return toResponse(role);
    }

    @Override
    @Transactional
    public RoleResponse createRole(CreateRoleRequest request) {
        if (roleRepository.existsByRoleName(request.getRoleName())) {
            throw new AppException(ErrorCode.ROLE_ALREADY_EXISTS);
        }

        Set<PermissionEntity> permissions = resolvePermissions(request.getPermissionIds());

        RoleEntity role = RoleEntity.builder()
                .roleName(request.getRoleName())
                .description(request.getDescription())
                .permissions(permissions)
                .build();

        return toResponse(roleRepository.save(role));
    }

    @Override
    @Transactional
    public RoleResponse updateRole(Long id, UpdateRoleRequest request) {
        RoleEntity role = roleRepository.findByIdWithPermissions(id)
                .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));

        role.setDescription(request.getDescription());
        role.setPermissions(resolvePermissions(request.getPermissionIds()));

        return toResponse(roleRepository.save(role));
    }

    @Override
    @Transactional
    public void deleteRole(Long id) {
        RoleEntity role = roleRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));

        if (PROTECTED_ROLES.contains(role.getRoleName())) {
            throw new AppException(ErrorCode.ROLE_PROTECTED);
        }

        roleRepository.delete(role);
    }

    // ── Private Helpers ───────────────────────────────────────────────────

    private Set<PermissionEntity> resolvePermissions(Set<Long> permissionIds) {
        if (permissionIds == null || permissionIds.isEmpty()) {
            return new HashSet<>();
        }

        Set<PermissionEntity> permissions = permissionRepository.findAllByPermissionIdIn(permissionIds);
        if (permissions.size() != permissionIds.size()) {
            throw new AppException(ErrorCode.PERMISSION_NOT_FOUND);
        }
        return permissions;
    }

    private RoleResponse toResponse(RoleEntity entity) {
        Set<PermissionResponse> permissionResponses = entity.getPermissions().stream()
                .map(p -> PermissionResponse.builder()
                        .permissionId(p.getPermissionId())
                        .permissionName(p.getPermissionName())
                        .description(p.getDescription())
                        .createdAt(p.getCreatedAt())
                        .build())
                .collect(Collectors.toSet());

        return RoleResponse.builder()
                .roleId(entity.getRoleId())
                .roleName(entity.getRoleName())
                .description(entity.getDescription())
                .createdAt(entity.getCreatedAt())
                .permissions(permissionResponses)
                .build();
    }
}
