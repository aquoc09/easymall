package com.quocnva.easymall.dtos.response.product;

import lombok.*;

import java.math.BigDecimal;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductVariantResponse {

    private Long variantId;
    private BigDecimal price;
    private BigDecimal costPrice;

    /**
     * variantAttributes trả về dạng Map để client dễ xử lý.
     * Mapper parse JSON string → Map<String, String>.
     */
    private Map<String, String> variantAttributes;

    private String skuCode;
    private String variantImage;
    private Integer stockQuantity;
    private Boolean isActive;
    private Integer lockedStock;
}
