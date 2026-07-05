package com.quocnva.easymall.dtos.request.product;

import com.quocnva.easymall.enums.TargetGender;
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
public class ProductUpdateRequest {

    @Size(max = 150, message = "{validation.productName.size}")
    private String productName;

    @Size(max = 2000, message = "{validation.productDescription.size}")
    private String productDescription;

    private Boolean inPopular;

    private Boolean inStock;

    /** 0=Nữ, 1=Nam, 2=Unisex. Hỗ trợ cả dạng số (0,1,2) lẫn chuỗi ("FEMALE","MALE","UNISEX","OTHER"). */
    private TargetGender targetGender;

    @Min(value = 0, message = "{validation.maxOrderQuantity.min}")
    private Integer maxOrderQuantity;

    private String optionsConfig;

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

    /**
     * Nếu có, service thực hiện diff-orphan: xóa variant cũ không còn trong list,
     * tạo mới variant chưa có, cập nhật variant đã có (dựa vào variantId).
     */
    @Valid
    private List<ProductVariantRequest> variants;

    private List<ProductImageRequest> images;
}
