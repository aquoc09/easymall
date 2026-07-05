package com.quocnva.easymall.dtos.request.product;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductVariantRequest {

    @NotNull(message = "{validation.price.not-null}")
    @DecimalMin(value = "0.00", inclusive = false, message = "{validation.price.min}")
    private BigDecimal price;

    @NotNull(message = "{validation.costPrice.not-null}")
    @DecimalMin(value = "0.00", inclusive = false, message = "{validation.costPrice.min}")
    private BigDecimal costPrice;

    /**
     * Tổ hợp thuộc tính biến thể. Ví dụ: {"color": "NVY", "size": "M"}.
     * Mapper sẽ convert Map → JSON string (variantAttributes).
     * Đồng thời dùng để sinh SKU code qua SkuGenerator.
     * <p>
     * Có thể là null hoặc rỗng {} cho sản phẩm đơn giản (single-variant),
     * khi đó service sẽ dùng SKU pattern mặc định.
     */
    private Map<String, String> variantAttributes;

    /** Nếu null, service sẽ tự sinh qua SkuGenerator. */
    private String skuCode;

    @Size(max = 500, message = "{validation.variantImage.size}")
    private String variantImage;

    @Min(value = 0, message = "{validation.stockQuantity.min}")
    private Integer stockQuantity;
}
