package com.quocnva.easymall.dtos.response.product;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductImageResponse {

    private Long imageId;
    private String imageUrl;
    private Boolean isThumbnail;
    private Integer displayOrder;
}
