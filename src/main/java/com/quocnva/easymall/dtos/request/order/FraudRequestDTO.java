package com.quocnva.easymall.dtos.request.order;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FraudRequestDTO {

    @JsonProperty("order_total_amount")
    private Double orderTotalAmount;

    @JsonProperty("payment_method")
    private Integer paymentMethod;

    @JsonProperty("is_vpn_proxy")
    private Integer isVpnProxy;

    @JsonProperty("location_mismatch")
    private Integer locationMismatch;

    @JsonProperty("orders_per_device_24h")
    private Integer ordersPerDevice24h;

    @JsonProperty("account_age_days")
    private Integer accountAgeDays;

    @JsonProperty("reputation_score")
    private Double reputationScore;

    @JsonProperty("failed_payment_attempts_10m")
    private Integer failedPaymentAttempts10m;

    @JsonProperty("total_distinct_devices")
    private Integer totalDistinctDevices;

    @JsonProperty("return_rate")
    private Double returnRate;
}
