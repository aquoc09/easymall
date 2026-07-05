package com.quocnva.easymall.dtos.response.product;

import com.quocnva.easymall.enums.TargetGender;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductResponse {

    private Long productId;
    private String productSlug;
    private String productName;
    private String productDescription;
    private Boolean inPopular;
    private Boolean inStock;

    /** Serialize thành số (0/1/2) nhờ @JsonValue trong TargetGender enum. */
    private TargetGender targetGender;

    private Integer maxOrderQuantity;

    /**
     * options_config — trả về dạng Map<String, Object> cho flexible rendering.
     * Mapper parse JSON string → Map.
     */
    private Map<String, Object> optionsConfig;

    /**
     * product_tags — trả về List<String> thay vì JSON string.
     */
    private List<String> productTags;

    private Long categoryId;
    private BigDecimal weightKg;
    private BigDecimal lengthM;
    private BigDecimal widthM;
    private BigDecimal heightM;
    private LocalDateTime createdAt;

    private List<ProductVariantResponse> variants;
    private List<ProductImageResponse> images;
}
