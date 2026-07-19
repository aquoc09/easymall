package com.quocnva.easymall.controller;

import com.quocnva.easymall.util.Translator;

import com.quocnva.easymall.dtos.request.review.CreateReviewRequest;
import com.quocnva.easymall.dtos.request.review.UpdateReviewStatusRequest;
import com.quocnva.easymall.dtos.response.ApiResponse;
import com.quocnva.easymall.dtos.response.review.ReviewResponse;
import com.quocnva.easymall.dtos.response.review.ReviewSummaryResponse;
import com.quocnva.easymall.service.review.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    // ── USER ──────────────────────────────────────────────────────────────

    @PostMapping
    @PreAuthorize("@permissionChecker.has('review:create')")
    public ApiResponse<ReviewResponse> createReview(
            @Valid @RequestBody CreateReviewRequest request,
            Authentication authentication) {
        return ApiResponse.<ReviewResponse>builder()
                .result(reviewService.createReview(request, authentication.getName()))
                .message(Translator.toLocale("success.review.created"))
                .build();
    }

    @GetMapping("/me")
    @PreAuthorize("@permissionChecker.has('review:view')")
    public ApiResponse<Page<ReviewResponse>> getMyReviews(
            Authentication authentication,
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable) {
        return ApiResponse.<Page<ReviewResponse>>builder()
                .result(reviewService.getMyReviews(authentication.getName(), pageable))
                .build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@permissionChecker.has('review:delete')")
    public ApiResponse<Void> deleteReview(
            @PathVariable Long id,
            Authentication authentication) {
        reviewService.deleteReview(id, authentication.getName());
        return ApiResponse.<Void>builder()
                .message(Translator.toLocale("success.review.deleted"))
                .build();
    }

    // ── PUBLIC ────────────────────────────────────────────────────────────

    @GetMapping("/product/{productId}")
    public ApiResponse<Page<ReviewResponse>> getProductReviews(
            @PathVariable Long productId,
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable) {
        return ApiResponse.<Page<ReviewResponse>>builder()
                .result(reviewService.getProductReviews(productId, pageable))
                .build();
    }

    @GetMapping("/product/{productId}/summary")
    public ApiResponse<ReviewSummaryResponse> getProductReviewSummary(@PathVariable Long productId) {
        return ApiResponse.<ReviewSummaryResponse>builder()
                .result(reviewService.getProductReviewSummary(productId))
                .build();
    }

    // ── ADMIN ─────────────────────────────────────────────────────────────

    @PatchMapping("/{id}/status")
    @PreAuthorize("@permissionChecker.has('review:moderate')")
    public ApiResponse<ReviewResponse> updateReviewStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateReviewStatusRequest request) {
        return ApiResponse.<ReviewResponse>builder()
                .result(reviewService.updateReviewStatus(id, request))
                .message(Translator.toLocale("success.review.status-updated"))
                .build();
    }
}
