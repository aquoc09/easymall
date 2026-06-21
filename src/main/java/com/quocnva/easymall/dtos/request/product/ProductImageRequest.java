package com.quocnva.easymall.dtos.request.product;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductImageRequest {

    @NotBlank(message = "{validation.imageUrl.not-blank}")
    @Size(max = 500, message = "{validation.imageUrl.size}")
    private String imageUrl;

    private Boolean isThumbnail;

    @Min(value = 0, message = "{validation.displayOrder.min}")
    private Integer displayOrder;
}
