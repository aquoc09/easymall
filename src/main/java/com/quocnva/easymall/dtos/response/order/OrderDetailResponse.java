package com.quocnva.easymall.dtos.response.order;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
public class OrderDetailResponse {

    private Long orderDetailId;
    private Long variantId;
    private String skuCode;
    private String productName;
    private Object variantAttributes;
    private BigDecimal price;
    private Integer quantity;
    private BigDecimal totalMoney;
    private String itemStatus;
}
