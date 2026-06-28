package com.quocnva.easymall.dtos.response.review;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

/**
 * Tổng hợp thông tin rating của sản phẩm.
 * Tính toán động qua aggregate query, không cache vào bảng products.
 */
@Getter
@Setter
@Builder
public class ReviewSummaryResponse {

    private Long productId;

    /** Điểm trung bình (APPROVED reviews), null nếu chưa có review nào */
    private Double averageRating;

    /** Tổng số APPROVED reviews */
    private Long totalReviews;

    /**
     * Breakdown theo từng mức sao.
     * Key: số sao (1–5), Value: số lượng review.
     * Ví dụ: {5: 120, 4: 30, 3: 10, 2: 2, 1: 1}
     */
    private Map<Integer, Long> ratingBreakdown;
}
