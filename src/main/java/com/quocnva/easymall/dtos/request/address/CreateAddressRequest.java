package com.quocnva.easymall.dtos.request.address;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateAddressRequest {

    @NotBlank(message = "{validation.recipientName.not-blank}")
    private String recipientName;

    @NotBlank(message = "{validation.phone.not-blank}")
    @Pattern(regexp = "^[0-9]{10,11}$", message = "{validation.phone.pattern}")
    private String phone;

    @NotNull(message = "{validation.provinceId.not-null}")
    private Integer provinceId;

    @NotNull(message = "{validation.districtId.not-null}")
    private Integer districtId;

    @NotBlank(message = "{validation.wardCode.not-blank}")
    private String wardCode;

    @Size(max = 100, message = "{validation.streetNumber.size}")
    private String streetNumber;

    /** Nếu true, địa chỉ này sẽ được đặt làm mặc định */
    private Boolean isDefault = false;
}
