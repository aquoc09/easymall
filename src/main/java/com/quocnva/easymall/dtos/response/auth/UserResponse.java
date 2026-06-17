package com.quocnva.easymall.dtos.response.auth;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Getter
@Builder
public class UserResponse {

    private Long userId;
    private String email;
    private String fullName;
    private Short gender;
    private String phone;
    private LocalDate dob;
    private Boolean isActive;
    private OffsetDateTime createdAt;
    private String roleName;
}
