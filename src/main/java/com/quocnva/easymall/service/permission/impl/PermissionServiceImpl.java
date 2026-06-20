package com.quocnva.easymall.service.permission.impl;

import com.quocnva.easymall.dtos.request.permission.CreatePermissionRequest;
import com.quocnva.easymall.dtos.request.permission.UpdatePermissionRequest;
import com.quocnva.easymall.dtos.response.permission.PermissionResponse;
import com.quocnva.easymall.entity.PermissionEntity;
import com.quocnva.easymall.exception.AppException;
import com.quocnva.easymall.exception.ErrorCode;
import com.quocnva.easymall.repository.PermissionRepository;
import com.quocnva.easymall.service.permission.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PermissionServiceImpl implements PermissionService {

    private final PermissionRepository permissionRepository;

    @Override
    @Transactional(readOnly = true)
    public List<PermissionResponse> getAllPermissions() {
        return permissionRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public PermissionResponse createPermission(CreatePermissionRequest request) {
        if (permissionRepository.existsByPermissionName(request.getPermissionName())) {
            throw new AppException(ErrorCode.PERMISSION_ALREADY_EXISTS);
        }

        PermissionEntity entity = PermissionEntity.builder()
                .permissionName(request.getPermissionName())
                .description(request.getDescription())
                .build();

        return toResponse(permissionRepository.save(entity));
    }

    @Override
    @Transactional
    public PermissionResponse updatePermission(Long id, UpdatePermissionRequest request) {
        PermissionEntity entity = permissionRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PERMISSION_NOT_FOUND));

        entity.setDescription(request.getDescription());

        return toResponse(permissionRepository.save(entity));
    }

    @Override
    @Transactional
    public void deletePermission(Long id) {
        PermissionEntity entity = permissionRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PERMISSION_NOT_FOUND));

        permissionRepository.delete(entity);
    }

    // ── Private Helpers ───────────────────────────────────────────────────

    private PermissionResponse toResponse(PermissionEntity entity) {
        return PermissionResponse.builder()
                .permissionId(entity.getPermissionId())
                .permissionName(entity.getPermissionName())
                .description(entity.getDescription())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
