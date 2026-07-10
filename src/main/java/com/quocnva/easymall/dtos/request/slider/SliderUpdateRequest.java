package com.quocnva.easymall.dtos.request.slider;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SliderUpdateRequest {

    private String imageUrl;

    private String targetUrl;

    private Boolean isActive;

    private Integer displayOrder;
}
