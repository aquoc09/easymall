package com.quocnva.easymall.service.ghn;

import com.quocnva.easymall.dtos.response.ghn.GhnDistrictResponse;
import com.quocnva.easymall.dtos.response.ghn.GhnProvinceResponse;
import com.quocnva.easymall.dtos.response.ghn.GhnWardResponse;

import java.util.List;

public interface GhnMasterDataService {

    /** Lấy danh sách tỉnh/thành — cached Redis 24h */
    List<GhnProvinceResponse> getProvinces();

    /** Lấy danh sách quận/huyện theo tỉnh — cached Redis 24h */
    List<GhnDistrictResponse> getDistricts(Integer provinceId);

    /** Lấy danh sách phường/xã theo huyện — cached Redis 24h */
    List<GhnWardResponse> getWards(Integer districtId);

    /**
     * Tra cứu districtId từ wardCode.
     * Dùng để lấy fromDistrictId của kho từ ghn.ward-code trong config.
     */
    Integer getDistrictIdByWardCode(String wardCode, Integer districtId);
}
