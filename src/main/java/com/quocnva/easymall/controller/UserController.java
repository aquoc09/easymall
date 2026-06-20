package com.quocnva.easymall.controller;

import com.quocnva.easymall.dtos.request.user.CreateUserRequest;
import com.quocnva.easymall.dtos.request.user.UpdateUserRequest;
import com.quocnva.easymall.dtos.response.ApiResponse;
import com.quocnva.easymall.dtos.response.user.UserDetailResponse;
import com.quocnva.easymall.service.user.UserService;
import com.quocnva.easymall.util.Translator;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Handles user management endpoints (admin only).
 * Profile endpoints (/me) are in AuthController.
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    @PreAuthorize("@permissionChecker.has('user:read')")
    public ApiResponse<Page<UserDetailResponse>> getAllUsers(
            @PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.<Page<UserDetailResponse>>builder()
                .result(userService.getAllUsers(pageable))
                .build();
    }

    @GetMapping("/{id}")
    @PreAuthorize("@permissionChecker.has('user:read')")
    public ApiResponse<UserDetailResponse> getUserById(@PathVariable Long id) {
        return ApiResponse.<UserDetailResponse>builder()
                .result(userService.getUserById(id))
                .build();
    }

    @PostMapping
    @PreAuthorize("@permissionChecker.has('user:create')")
    public ApiResponse<UserDetailResponse> createUser(
            @RequestBody @Valid CreateUserRequest request) {
        return ApiResponse.<UserDetailResponse>builder()
                .message(Translator.toLocale("success.user.created"))
                .result(userService.createUser(request))
                .build();
    }

    @PutMapping("/{id}")
    @PreAuthorize("@permissionChecker.has('user:update')")
    public ApiResponse<UserDetailResponse> updateUser(
            @PathVariable Long id,
            @RequestBody @Valid UpdateUserRequest request) {
        return ApiResponse.<UserDetailResponse>builder()
                .message(Translator.toLocale("success.user.updated"))
                .result(userService.updateUser(id, request))
                .build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@permissionChecker.has('user:delete')")
    public ApiResponse<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ApiResponse.<Void>builder()
                .message(Translator.toLocale("success.user.deleted"))
                .build();
    }
}
