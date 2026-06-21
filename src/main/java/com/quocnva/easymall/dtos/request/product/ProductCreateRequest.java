package com.quocnva.easymall.dtos.request.product;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductCreateRequest {

    @NotBlank(message = "{validation.productName.not-blank}")
    @Size(max = 150, message = "{validation.productName.size}")
    private String productName;

    @Size(max = 2000, message = "{validation.productDescription.size}")
    private String productDescription;

    private Boolean inPopular;

    /** 0=Nữ, 1=Nam, 2=Unisex */
    @Min(value = 0, message = "{validation.targetGender.range}")
    @Max(value = 2, message = "{validation.targetGender.range}")
    private Integer targetGender;

    @Min(value = 0, message = "{validation.maxOrderQuantity.min}")
    private Integer maxOrderQuantity;

    /**
     * options_config — JSON string tự do từ client.
     * Service sẽ validate format JSON trước khi lưu.
     */
    private String optionsConfig;

    /**
     * product_tags — danh sách tag. Ví dụ: ["vintage", "oversize"].
     * Mapper convert List<String> → JSON string khi map vào entity.
     */
    private List<String> productTags;

    private Long categoryId;

    @DecimalMin(value = "0.01", message = "{validation.weight.min}")
    private BigDecimal weightKg;

    @DecimalMin(value = "0.01", message = "{validation.dimension.min}")
    private BigDecimal lengthM;

    @DecimalMin(value = "0.01", message = "{validation.dimension.min}")
    private BigDecimal widthM;

    @DecimalMin(value = "0.01", message = "{validation.dimension.min}")
    private BigDecimal heightM;

    @NotEmpty(message = "{validation.variants.not-empty}")
    @Valid
    private List<ProductVariantRequest> variants;

    private List<ProductImageRequest> images;
}
