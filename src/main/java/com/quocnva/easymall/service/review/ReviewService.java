package com.quocnva.easymall.service.review;

import com.quocnva.easymall.dtos.request.review.CreateReviewRequest;
import com.quocnva.easymall.dtos.request.review.UpdateReviewStatusRequest;
import com.quocnva.easymall.dtos.response.review.ReviewResponse;
import com.quocnva.easymall.dtos.response.review.ReviewSummaryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ReviewService {

    ReviewResponse createReview(CreateReviewRequest request, String userEmail);

    Page<ReviewResponse> getProductReviews(Long productId, Pageable pageable);

    ReviewSummaryResponse getProductReviewSummary(Long productId);

    Page<ReviewResponse> getMyReviews(String userEmail, Pageable pageable);

    ReviewResponse updateReviewStatus(Long reviewId, UpdateReviewStatusRequest request);

    void deleteReview(Long reviewId, String userEmail);
}
