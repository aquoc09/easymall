package com.quocnva.easymall.dtos.request.slider;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SliderCreateRequest {

    @NotBlank(message = "{error.slider.image-url-required}")
    private String imageUrl;

    private String targetUrl;

    @Builder.Default
    private Boolean isActive = true;

    @Builder.Default
    private Integer displayOrder = 0;
}
