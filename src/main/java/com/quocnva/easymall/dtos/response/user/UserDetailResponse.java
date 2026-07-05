package com.quocnva.easymall.dtos.response.user;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Getter
@Builder
public class UserDetailResponse {

    private Long userId;
    private String email;
    private String fullName;
    private Short gender;
    private String phone;
    private LocalDate dob;
    private Boolean isActive;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private String roleName;
    private Long roleId;
    private String avatar;
}
