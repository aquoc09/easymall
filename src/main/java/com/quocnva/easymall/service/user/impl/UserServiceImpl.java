package com.quocnva.easymall.service.user.impl;

import com.quocnva.easymall.dtos.request.user.CreateUserRequest;
import com.quocnva.easymall.dtos.request.user.UpdateUserRequest;
import com.quocnva.easymall.dtos.response.auth.UserResponse;
import com.quocnva.easymall.dtos.response.user.UserDetailResponse;
import com.quocnva.easymall.entity.RoleEntity;
import com.quocnva.easymall.entity.UserEntity;
import com.quocnva.easymall.exception.AppException;
import com.quocnva.easymall.exception.ErrorCode;
import com.quocnva.easymall.repository.RoleRepository;
import com.quocnva.easymall.repository.UserRepository;
import com.quocnva.easymall.service.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    // ── Profile ───────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public UserResponse getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        return UserResponse.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .gender(user.getGender())
                .phone(user.getPhone())
                .dob(user.getDob())
                .isActive(user.getIsActive())
                .createdAt(user.getCreatedAt())
                .roleName(user.getRole() != null ? user.getRole().getRoleName() : null)
                .build();
    }

    // ── Admin CRUD ────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public Page<UserDetailResponse> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable)
                .map(this::toDetailResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetailResponse getUserById(Long id) {
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        return toDetailResponse(user);
    }

    @Override
    @Transactional
    public UserDetailResponse createUser(CreateUserRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AppException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        RoleEntity role = roleRepository.findById(request.getRoleId())
                .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));

        UserEntity user = UserEntity.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .isActive(true)
                .role(role)
                .build();

        return toDetailResponse(userRepository.save(user));
    }

    @Override
    @Transactional
    public UserDetailResponse updateUser(Long id, UpdateUserRequest request) {
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (request.getFullName() != null) {
            user.setFullName(request.getFullName());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        if (request.getGender() != null) {
            user.setGender(request.getGender());
        }
        if (request.getDob() != null) {
            user.setDob(request.getDob());
        }
        if (request.getIsActive() != null) {
            user.setIsActive(request.getIsActive());
        }
        if (request.getRoleId() != null) {
            RoleEntity role = roleRepository.findById(request.getRoleId())
                    .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));
            user.setRole(role);
        }

        return toDetailResponse(userRepository.save(user));
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        // Soft delete — set isActive = false
        user.setIsActive(false);
        userRepository.save(user);
    }

    // ── Private Helpers ───────────────────────────────────────────────────

    private UserDetailResponse toDetailResponse(UserEntity user) {
        return UserDetailResponse.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .gender(user.getGender())
                .phone(user.getPhone())
                .dob(user.getDob())
                .isActive(user.getIsActive())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .roleName(user.getRole() != null ? user.getRole().getRoleName() : null)
                .roleId(user.getRole() != null ? user.getRole().getRoleId() : null)
                .build();
    }
}
