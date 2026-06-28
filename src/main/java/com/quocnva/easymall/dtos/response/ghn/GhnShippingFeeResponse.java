package com.quocnva.easymall.dtos.response.ghn;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class GhnShippingFeeResponse {

    /** Tổng phí ship (VND) */
    private Integer total;

    /** Phí dịch vụ (VND) */
    private Integer serviceFee;

    /** Phí bảo hiểm (VND) */
    private Integer insuranceFee;

    /** service_id đã được dùng để tính */
    private Integer serviceId;

    /** Tên gói dịch vụ (vd: "Giao hàng tiết kiệm") */
    private String shortName;
}
