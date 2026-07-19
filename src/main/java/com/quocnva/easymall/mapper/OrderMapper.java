package com.quocnva.easymall.mapper;

import com.quocnva.easymall.dtos.response.address.AddressResponse;
import com.quocnva.easymall.dtos.response.order.OrderDetailResponse;
import com.quocnva.easymall.dtos.response.order.OrderResponse;
import com.quocnva.easymall.dtos.response.order.OrderSummaryResponse;
import com.quocnva.easymall.entity.AddressEntity;
import com.quocnva.easymall.entity.OrderDetailEntity;
import com.quocnva.easymall.entity.OrderEntity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class OrderMapper {

    public AddressResponse toAddressResponse(AddressEntity address) {
        if (address == null) return null;
        return AddressResponse.builder()
                .addressId(address.getAddressId())
                .recipientName(address.getRecipientName())
                .phone(address.getPhone())
                .fullAddress(address.getFullAddress())
                .streetNumber(address.getStreetNumber())
                .provinceId(address.getProvinceId())
                .districtId(address.getDistrictId())
                .wardCode(address.getWardCode())
                .build();
    }

    @org.springframework.beans.factory.annotation.Value("${storage.base-url}")
    private String baseUrl;

    public OrderDetailResponse toDetailResponse(OrderDetailEntity detail) {
        Long variantId = detail.getVariant() != null ? detail.getVariant().getVariantId() : null;
        String imageUrl = detail.getVariantImage();
        if (imageUrl != null && !imageUrl.startsWith("http")) {
            imageUrl = baseUrl + "/" + imageUrl;
        }

        return OrderDetailResponse.builder()
                .orderDetailId(detail.getOrderDetailId())
                .variantId(variantId)
                .skuCode(detail.getSkuCode())
                .productName(detail.getProductName())
                .variantAttributes(detail.getVariantAttributes())
                .variantImage(imageUrl)
                .price(detail.getOrderDetailPrice())
                .quantity(detail.getNumOfProduct())
                .totalMoney(detail.getTotalMoney())
                .itemStatus(detail.getItemStatus())
                .build();
    }

    public OrderResponse toResponse(OrderEntity order) {
        List<OrderDetailResponse> items = order.getOrderDetails().stream()
                .map(this::toDetailResponse)
                .collect(Collectors.toList());

        return OrderResponse.builder()
                .orderId(order.getOrderId())
                .orderDate(order.getOrderDate())
                .orderStatus(order.getOrderStatus())
                .paymentMethod(order.getPaymentMethod())
                .shippingMethod(order.getShippingMethod())
                .trackingNumber(order.getTrackingNumber())
                .note(order.getNote())
                .totalProductMoney(order.getTotalProductMoney())
                .originalShippingFee(order.getOriginalShippingFee())
                .shopDiscountAmount(order.getShopDiscountAmount())
                .shippingDiscountAmount(order.getShippingDiscountAmount())
                .paymentDiscountAmount(order.getPaymentDiscountAmount())
                .finalPaymentMoney(order.getFinalPaymentMoney())
                .address(toAddressResponse(order.getAddress()))
                .items(items)
                .build();
    }

    public OrderSummaryResponse toSummaryResponse(OrderEntity order) {
        return OrderSummaryResponse.builder()
                .orderId(order.getOrderId())
                .orderDate(order.getOrderDate())
                .orderStatus(order.getOrderStatus())
                .paymentMethod(order.getPaymentMethod())
                .finalPaymentMoney(order.getFinalPaymentMoney())
                .itemCount(order.getOrderDetails().size())
                .build();
    }
}
