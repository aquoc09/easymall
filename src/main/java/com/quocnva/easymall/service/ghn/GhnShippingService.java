package com.quocnva.easymall.service.ghn;

import com.quocnva.easymall.dtos.request.ghn.ShippingFeeRequest;
import com.quocnva.easymall.dtos.response.ghn.GhnServiceResponse;
import com.quocnva.easymall.dtos.response.ghn.GhnShippingFeeResponse;

import java.util.List;

public interface GhnShippingService {

    /** Lấy danh sách service hợp lệ cho tuyến toDistrict */
    List<GhnServiceResponse> getAvailableServices(Integer toDistrictId);

    /** Tính phí ship (2 bước: available-services → fee) */
    GhnShippingFeeResponse calculateFee(ShippingFeeRequest request);
}
