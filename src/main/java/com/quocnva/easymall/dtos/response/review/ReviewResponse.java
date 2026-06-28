package com.quocnva.easymall.dtos.response.review;

import com.quocnva.easymall.enums.ReviewStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.List;

@Getter
@Setter
@Builder
public class ReviewResponse {

    private Long reviewId;

    /** null nếu review anonymous (order đã bị xóa) */
    private Long orderId;

    private Long userId;

    /** null nếu user đã bị xóa */
    private String userFullName;

    private Long productId;
    private String productName;

    private Integer rating;
    private String comment;
    private ReviewStatus reviewStatus;

    private List<ReviewImageResponse> images;

    private OffsetDateTime createdAt;
}
