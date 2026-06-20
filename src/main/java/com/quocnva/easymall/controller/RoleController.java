package com.quocnva.easymall.controller;

import com.quocnva.easymall.dtos.request.role.CreateRoleRequest;
import com.quocnva.easymall.dtos.request.role.UpdateRoleRequest;
import com.quocnva.easymall.dtos.response.ApiResponse;
import com.quocnva.easymall.dtos.response.role.RoleResponse;
import com.quocnva.easymall.service.role.RoleService;
import com.quocnva.easymall.util.Translator;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @GetMapping
    @PreAuthorize("@permissionChecker.has('role:read')")
    public ApiResponse<List<RoleResponse>> getAllRoles() {
        return ApiResponse.<List<RoleResponse>>builder()
                .result(roleService.getAllRoles())
                .build();
    }

    @GetMapping("/{id}")
    @PreAuthorize("@permissionChecker.has('role:read')")
    public ApiResponse<RoleResponse> getRoleById(@PathVariable Long id) {
        return ApiResponse.<RoleResponse>builder()
                .result(roleService.getRoleById(id))
                .build();
    }

    @PostMapping
    @PreAuthorize("@permissionChecker.has('role:create')")
    public ApiResponse<RoleResponse> createRole(
            @RequestBody @Valid CreateRoleRequest request) {
        return ApiResponse.<RoleResponse>builder()
                .message(Translator.toLocale("success.role.created"))
                .result(roleService.createRole(request))
                .build();
    }

    @PutMapping("/{id}")
    @PreAuthorize("@permissionChecker.has('role:update')")
    public ApiResponse<RoleResponse> updateRole(
            @PathVariable Long id,
            @RequestBody @Valid UpdateRoleRequest request) {
        return ApiResponse.<RoleResponse>builder()
                .message(Translator.toLocale("success.role.updated"))
                .result(roleService.updateRole(id, request))
                .build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@permissionChecker.has('role:delete')")
    public ApiResponse<Void> deleteRole(@PathVariable Long id) {
        roleService.deleteRole(id);
        return ApiResponse.<Void>builder()
                .message(Translator.toLocale("success.role.deleted"))
                .build();
    }
}
