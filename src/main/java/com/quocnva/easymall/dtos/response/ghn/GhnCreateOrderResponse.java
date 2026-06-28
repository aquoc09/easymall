package com.quocnva.easymall.dtos.response.ghn;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class GhnCreateOrderResponse {

    /** Mã đơn GHN — dùng làm tracking_number trong orders */
    private String orderCode;

    /** Tổng phí ship GHN tính (VND) */
    private Integer totalFee;

    /** Thời gian dự kiến giao (ISO datetime string từ GHN) */
    private String expectedDeliveryTime;
}
