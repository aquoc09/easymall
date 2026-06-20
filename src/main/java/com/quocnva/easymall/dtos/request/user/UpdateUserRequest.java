package com.quocnva.easymall.dtos.request.user;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class UpdateUserRequest {

    private String fullName;
    private String phone;
    private Short gender;
    private LocalDate dob;
    private Long roleId;
    private Boolean isActive;
}
