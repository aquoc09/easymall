package com.quocnva.easymall.service.address;

import com.quocnva.easymall.dtos.request.address.CreateAddressRequest;
import com.quocnva.easymall.dtos.request.address.UpdateAddressRequest;
import com.quocnva.easymall.dtos.response.address.AddressResponse;

import java.util.List;

/**
 * Address management service interface.
 * Encapsulates: list, create, update, delete, set default address.
 */
public interface AddressService {

    /** Lấy tất cả địa chỉ của user hiện tại */
    List<AddressResponse> getMyAddresses(String userEmail);

    /** Tạo địa chỉ mới (tối đa 5 địa chỉ/user) */
    AddressResponse createAddress(CreateAddressRequest request, String userEmail);

    /** Cập nhật địa chỉ (chỉ owner) */
    AddressResponse updateAddress(Long addressId, UpdateAddressRequest request, String userEmail);

    /** Xóa địa chỉ (chỉ owner, không xóa địa chỉ đang mặc định) */
    void deleteAddress(Long addressId, String userEmail);

    /** Đặt địa chỉ làm mặc định */
    AddressResponse setDefault(Long addressId, String userEmail);
}
