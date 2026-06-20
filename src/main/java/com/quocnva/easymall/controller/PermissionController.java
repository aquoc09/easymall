package com.quocnva.easymall.controller;

import com.quocnva.easymall.dtos.request.permission.CreatePermissionRequest;
import com.quocnva.easymall.dtos.request.permission.UpdatePermissionRequest;
import com.quocnva.easymall.dtos.response.ApiResponse;
import com.quocnva.easymall.dtos.response.permission.PermissionResponse;
import com.quocnva.easymall.service.permission.PermissionService;
import com.quocnva.easymall.util.Translator;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/permissions")
@RequiredArgsConstructor
public class PermissionController {

    private final PermissionService permissionService;

    @GetMapping
    @PreAuthorize("@permissionChecker.has('permission:read')")
    public ApiResponse<List<PermissionResponse>> getAllPermissions() {
        return ApiResponse.<List<PermissionResponse>>builder()
                .result(permissionService.getAllPermissions())
                .build();
    }

    @PostMapping
    @PreAuthorize("@permissionChecker.has('permission:create')")
    public ApiResponse<PermissionResponse> createPermission(
            @RequestBody @Valid CreatePermissionRequest request) {
        return ApiResponse.<PermissionResponse>builder()
                .message(Translator.toLocale("success.permission.created"))
                .result(permissionService.createPermission(request))
                .build();
    }

    @PutMapping("/{id}")
    @PreAuthorize("@permissionChecker.has('permission:update')")
    public ApiResponse<PermissionResponse> updatePermission(
            @PathVariable Long id,
            @RequestBody @Valid UpdatePermissionRequest request) {
        return ApiResponse.<PermissionResponse>builder()
                .message(Translator.toLocale("success.permission.updated"))
                .result(permissionService.updatePermission(id, request))
                .build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@permissionChecker.has('permission:delete')")
    public ApiResponse<Void> deletePermission(@PathVariable Long id) {
        permissionService.deletePermission(id);
        return ApiResponse.<Void>builder()
                .message(Translator.toLocale("success.permission.deleted"))
                .build();
    }
}
