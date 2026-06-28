package com.quocnva.easymall.dtos.response.ghn;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class GhnServiceResponse {

    @JsonProperty("service_id")
    private Integer serviceId;

    @JsonProperty("service_type_id")
    private Integer serviceTypeId;

    @JsonProperty("short_name")
    private String shortName;
}
