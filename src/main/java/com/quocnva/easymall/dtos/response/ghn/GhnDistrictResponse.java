package com.quocnva.easymall.dtos.response.ghn;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class GhnDistrictResponse {

    @JsonProperty("DistrictID")
    private Integer districtId;

    @JsonProperty("DistrictName")
    private String districtName;

    @JsonProperty("ProvinceID")
    private Integer provinceId;
}
