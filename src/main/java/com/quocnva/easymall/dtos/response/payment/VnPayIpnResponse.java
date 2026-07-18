package com.quocnva.easymall.dtos.response.payment;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class VnPayIpnResponse {
    @JsonProperty("RspCode")
    String rspCode;

    @JsonProperty("Message")
    String message;
}
