package com.quocnva.easymall.service.address.impl;

import com.quocnva.easymall.dtos.request.address.CreateAddressRequest;
import com.quocnva.easymall.dtos.request.address.UpdateAddressRequest;
import com.quocnva.easymall.dtos.response.ghn.GhnDistrictResponse;
import com.quocnva.easymall.dtos.response.ghn.GhnProvinceResponse;
import com.quocnva.easymall.dtos.response.ghn.GhnWardResponse;
import com.quocnva.easymall.dtos.response.order.AddressResponse;
import com.quocnva.easymall.entity.AddressEntity;
import com.quocnva.easymall.entity.UserEntity;
import com.quocnva.easymall.exception.AppException;
import com.quocnva.easymall.exception.ErrorCode;
import com.quocnva.easymall.repository.AddressRepository;
import com.quocnva.easymall.repository.UserRepository;
import com.quocnva.easymall.service.address.AddressService;
import com.quocnva.easymall.service.ghn.GhnMasterDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AddressServiceImpl implements AddressService {

    private static final int MAX_ADDRESSES = 5;

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;
    private final GhnMasterDataService ghnMasterDataService;

    // ── Read ──────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<AddressResponse> getMyAddresses(String userEmail) {
        UserEntity user = findUserOrThrow(userEmail);
        return addressRepository.findAllByUserUserId(user.getUserId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // ── Create ────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public AddressResponse createAddress(CreateAddressRequest request, String userEmail) {
        UserEntity user = findUserOrThrow(userEmail);

        // Giới hạn 5 địa chỉ
        if (addressRepository.countByUserUserId(user.getUserId()) >= MAX_ADDRESSES) {
            throw new AppException(ErrorCode.ADDRESS_MAX_LIMIT);
        }

        // Lấy tên tỉnh/huyện/xã từ GHN master data (đã cached trong Redis)
        String provinceName = resolveProvinceName(request.getProvinceId());
        String districtName = resolveDistrictName(request.getProvinceId(), request.getDistrictId());
        String wardName = resolveWardName(request.getDistrictId(), request.getWardCode());

        // Nếu set làm default → unset các địa chỉ cũ
        if (Boolean.TRUE.equals(request.getIsDefault())) {
            unsetAllDefaults(user.getUserId());
        }

        AddressEntity entity = AddressEntity.builder()
                .user(user)
                .recipientName(request.getRecipientName())
                .phone(request.getPhone())
                .provinceId(request.getProvinceId())
                .provinceName(provinceName)
                .districtId(request.getDistrictId())
                .districtName(districtName)
                .wardCode(request.getWardCode())
                .wardName(wardName)
                .streetNumber(request.getStreetNumber())
                .isDefault(Boolean.TRUE.equals(request.getIsDefault()))
                .build();

        // Nếu là địa chỉ đầu tiên → tự động set default
        if (addressRepository.countByUserUserId(user.getUserId()) == 0) {
            entity.setIsDefault(true);
        }

        return toResponse(addressRepository.save(entity));
    }

    // ── Update ────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public AddressResponse updateAddress(Long addressId, UpdateAddressRequest request, String userEmail) {
        AddressEntity entity = findAddressAndValidateOwnership(addressId, userEmail);

        if (request.getRecipientName() != null) entity.setRecipientName(request.getRecipientName());
        if (request.getPhone() != null) entity.setPhone(request.getPhone());
        if (request.getStreetNumber() != null) entity.setStreetNumber(request.getStreetNumber());

        // Nếu thay đổi địa chính → cần cập nhật tên
        if (request.getProvinceId() != null) {
            entity.setProvinceId(request.getProvinceId());
            entity.setProvinceName(resolveProvinceName(request.getProvinceId()));
        }
        if (request.getDistrictId() != null) {
            Integer provinceId = request.getProvinceId() != null ? request.getProvinceId() : entity.getProvinceId();
            entity.setDistrictId(request.getDistrictId());
            entity.setDistrictName(resolveDistrictName(provinceId, request.getDistrictId()));
        }
        if (request.getWardCode() != null) {
            entity.setWardCode(request.getWardCode());
            entity.setWardName(resolveWardName(entity.getDistrictId(), request.getWardCode()));
        }

        // Set default
        if (Boolean.TRUE.equals(request.getIsDefault())) {
            unsetAllDefaults(entity.getUser().getUserId());
            entity.setIsDefault(true);
        }

        return toResponse(addressRepository.save(entity));
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public void deleteAddress(Long addressId, String userEmail) {
        AddressEntity entity = findAddressAndValidateOwnership(addressId, userEmail);
        addressRepository.delete(entity);
    }

    // ── Set Default ───────────────────────────────────────────────────────────

    @Override
    @Transactional
    public AddressResponse setDefault(Long addressId, String userEmail) {
        AddressEntity entity = findAddressAndValidateOwnership(addressId, userEmail);
        unsetAllDefaults(entity.getUser().getUserId());
        entity.setIsDefault(true);
        return toResponse(addressRepository.save(entity));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private UserEntity findUserOrThrow(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }

    private AddressEntity findAddressAndValidateOwnership(Long addressId, String userEmail) {
        UserEntity user = findUserOrThrow(userEmail);
        return addressRepository.findByAddressIdAndUser_UserId(addressId, user.getUserId())
                .orElseThrow(() -> new AppException(ErrorCode.ADDRESS_OWNERSHIP_DENIED));
    }

    private void unsetAllDefaults(Long userId) {
        List<AddressEntity> addresses = addressRepository.findAllByUserUserId(userId);
        addresses.forEach(a -> a.setIsDefault(false));
        addressRepository.saveAll(addresses);
    }

    private String resolveProvinceName(Integer provinceId) {
        return ghnMasterDataService.getProvinces().stream()
                .filter(p -> provinceId.equals(p.getProvinceId()))
                .map(GhnProvinceResponse::getProvinceName)
                .findFirst()
                .orElse("");
    }

    private String resolveDistrictName(Integer provinceId, Integer districtId) {
        return ghnMasterDataService.getDistricts(provinceId).stream()
                .filter(d -> districtId.equals(d.getDistrictId()))
                .map(GhnDistrictResponse::getDistrictName)
                .findFirst()
                .orElse("");
    }

    private String resolveWardName(Integer districtId, String wardCode) {
        return ghnMasterDataService.getWards(districtId).stream()
                .filter(w -> wardCode.equals(w.getWardCode()))
                .map(GhnWardResponse::getWardName)
                .findFirst()
                .orElse("");
    }

    private AddressResponse toResponse(AddressEntity entity) {
        return AddressResponse.builder()
                .addressId(entity.getAddressId())
                .recipientName(entity.getRecipientName())
                .phone(entity.getPhone())
                .provinceId(entity.getProvinceId())
                .provinceName(entity.getProvinceName())
                .districtId(entity.getDistrictId())
                .districtName(entity.getDistrictName())
                .wardCode(entity.getWardCode())
                .wardName(entity.getWardName())
                .streetNumber(entity.getStreetNumber())
                .isDefault(entity.getIsDefault())
                .build();
    }
}
