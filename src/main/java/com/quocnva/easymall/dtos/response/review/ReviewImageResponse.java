package com.quocnva.easymall.dtos.response.review;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class ReviewImageResponse {
    private Long reviewImageId;
    private String imageUrl;
}
