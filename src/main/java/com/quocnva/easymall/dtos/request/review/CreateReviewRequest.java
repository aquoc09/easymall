package com.quocnva.easymall.dtos.request.review;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.List;

@Data
public class CreateReviewRequest {

    @NotNull(message = "{validation.productId.not-null}")
    private Long productId;

    @NotNull(message = "{validation.orderId.not-null}")
    private Long orderId;

    @NotNull(message = "{validation.rating.not-null}")
    @Min(value = 1, message = "{validation.rating.min}")
    @Max(value = 5, message = "{validation.rating.max}")
    private Integer rating;

    @Size(max = 2000, message = "{validation.comment.size}")
    private String comment;

    /** Danh sách URL ảnh (upload lên Cloudinary trước, truyền URL vào đây) */
    private List<@NotBlank String> imageUrls;
}
