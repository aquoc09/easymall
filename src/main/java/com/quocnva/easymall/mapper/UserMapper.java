package com.quocnva.easymall.mapper;

import com.quocnva.easymall.dtos.response.user.UserDetailResponse;
import com.quocnva.easymall.entity.UserEntity;
import org.springframework.stereotype.Component;

/**
 * Converts {@link UserEntity} to {@link UserDetailResponse}.
 * Note: avatarUrl is NOT populated here — URL building requires the S3 base-url
 * which belongs in the service layer via {@code buildAvatarUrl()}.
 */
@Component
public class UserMapper {

    public UserDetailResponse toResponse(UserEntity entity) {
        if (entity == null) return null;
        return UserDetailResponse.builder()
                .userId(entity.getUserId())
                .email(entity.getEmail())
                .fullName(entity.getFullName())
                .gender(entity.getGender())
                .phone(entity.getPhone())
                .dob(entity.getDob())
                .isActive(entity.getIsActive())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .roleName(entity.getRole() != null ? entity.getRole().getRoleName() : null)
                .roleId(entity.getRole() != null ? entity.getRole().getRoleId() : null)
                .build();
    }
}
