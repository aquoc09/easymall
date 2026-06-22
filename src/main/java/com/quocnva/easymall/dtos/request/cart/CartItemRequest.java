package com.quocnva.easymall.dtos.request.cart;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartItemRequest {

    @NotNull(message = "validation.variantId.not-null")
    private Long variantId;

    @NotNull(message = "validation.quantity.not-null")
    @Min(value = 1, message = "validation.quantity.min")
    private Integer quantity;

    private String note;
}
