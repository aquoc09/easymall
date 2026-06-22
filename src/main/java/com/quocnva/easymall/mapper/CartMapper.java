package com.quocnva.easymall.mapper;

import com.quocnva.easymall.dtos.response.cart.CartItemResponse;
import com.quocnva.easymall.entity.CartItemEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CartMapper {

    /**
     * Ánh xạ CartItemEntity → CartItemResponse.
     * Các field phức tạp (productName, variantImage, available, unavailableReason)
     * được set thủ công trong CartServiceImpl.
     */
    @Mapping(target = "variantId",         source = "variant.variantId")
    @Mapping(target = "price",             source = "variant.price")
    @Mapping(target = "variantAttributes", source = "variant.variantAttributes")
    @Mapping(target = "variantImage",      source = "variant.variantImage")
    @Mapping(target = "productName",       source = "variant.product.productName")
    @Mapping(target = "available",         ignore = true)   // set thủ công
    @Mapping(target = "unavailableReason", ignore = true)   // set thủ công
    CartItemResponse toCartItemResponse(CartItemEntity entity);
}
