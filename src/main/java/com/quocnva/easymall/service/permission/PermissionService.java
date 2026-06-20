package com.quocnva.easymall.service.permission;

import com.quocnva.easymall.dtos.request.permission.CreatePermissionRequest;
import com.quocnva.easymall.dtos.request.permission.UpdatePermissionRequest;
import com.quocnva.easymall.dtos.response.permission.PermissionResponse;

import java.util.List;

public interface PermissionService {

    List<PermissionResponse> getAllPermissions();

    PermissionResponse createPermission(CreatePermissionRequest request);

    PermissionResponse updatePermission(Long id, UpdatePermissionRequest request);

    void deletePermission(Long id);
}
