package com.quocnva.easymall.dtos.request.ghn;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Payload GHN gửi về server qua webhook khi trạng thái đơn thay đổi.
 * GHN luôn POST HTTP 200, body chứa thông tin đơn hàng.
 */
@Data
public class GhnWebhookRequest {

    @JsonProperty("OrderCode")
    private String orderCode;

    @JsonProperty("Status")
    private String status;

    @JsonProperty("ExCode")
    private String exCode;

    @JsonProperty("Reason")
    private String reason;

    @JsonProperty("Time")
    private String time;
}
