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
    private String fullAddress;
    private String streetNumber;
    private Integer provinceId;
    private Integer districtId;
    private String wardCode;
}
