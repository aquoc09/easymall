package com.quocnva.easymall.dtos.response.order;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class AddressResponse {

    private Long addressId;
    private String recipientName;
    private String phone;

    private Integer provinceId;
    private String provinceName;

    private Integer districtId;
    private String districtName;

    private String wardCode;
    private String wardName;

    private String streetNumber;
    private String fullAddress;
    private Boolean isDefault;
}
