package com.quocnva.easymall.dtos.response.cart;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartItemResponse {

    private Long cartItemId;
    private Long variantId;
    private String productName;
    private String variantAttributes;
    private String variantImage;
    private BigDecimal price;
    private Integer quantity;
    private BigDecimal totalMoney;
    private String note;

    /**
     * true nếu item có thể mua được (variant active, còn hàng).
     * false nếu bị vô hiệu hóa (hết hàng, bị khóa...).
     */
    private Boolean available;

    /**
     * Lý do không khả dụng: "OUT_OF_STOCK" | "BANNED" | null (nếu available = true).
     */
    private String unavailableReason;
}
