package com.quocnva.easymall.dtos.request.address;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateAddressRequest {

    private String recipientName;

    @Pattern(regexp = "^[0-9]{10,11}$", message = "{validation.phone.pattern}")
    private String phone;

    private Integer provinceId;

    private Integer districtId;

    private String wardCode;

    @Size(max = 100, message = "{validation.streetNumber.size}")
    private String streetNumber;

    private Boolean isDefault;
}
