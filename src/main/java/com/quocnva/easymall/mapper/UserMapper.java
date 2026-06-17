package com.quocnva.easymall.mapper;

import com.quocnva.easymall.dtos.response.UserResponse;
import com.quocnva.easymall.entity.UserEntity;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserResponse toResponse(UserEntity entity) {
        if (entity == null) return null;
        return UserResponse.builder()
                .userId(entity.getUserId())
                .email(entity.getEmail())
                .fullName(entity.getFullName())
                .gender(entity.getGender())
                .phone(entity.getPhone())
                .dob(entity.getDob())
                .isActive(entity.getIsActive())
                .createdAt(entity.getCreatedAt())
                .roleName(entity.getRole() != null ? entity.getRole().getRoleName() : null)
                .build();
    }
}
