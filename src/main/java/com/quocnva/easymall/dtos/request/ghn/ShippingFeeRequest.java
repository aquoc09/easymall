package com.quocnva.easymall.dtos.request.ghn;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ShippingFeeRequest {

    @NotNull(message = "{validation.districtId.not-null}")
    private Integer toDistrictId;

    private String toWardCode;

    /** Tổng trọng lượng (gram) */
    @NotNull(message = "{validation.quantity.not-null}")
    private Integer weightGram;

    /**
     * Nếu client không truyền, service sẽ tự chọn service_id rẻ nhất
     * từ available-services.
     */
    private Integer serviceId;
}
