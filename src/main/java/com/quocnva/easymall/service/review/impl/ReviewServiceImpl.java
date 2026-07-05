package com.quocnva.easymall.service.review.impl;

import com.quocnva.easymall.dtos.request.review.CreateReviewRequest;
import com.quocnva.easymall.dtos.request.review.UpdateReviewStatusRequest;
import com.quocnva.easymall.dtos.response.review.ReviewImageResponse;
import com.quocnva.easymall.dtos.response.review.ReviewResponse;
import com.quocnva.easymall.dtos.response.review.ReviewSummaryResponse;
import com.quocnva.easymall.entity.*;
import com.quocnva.easymall.enums.OrderStatus;
import com.quocnva.easymall.enums.ReviewStatus;
import com.quocnva.easymall.exception.AppException;
import com.quocnva.easymall.exception.ErrorCode;
import com.quocnva.easymall.repository.*;
import com.quocnva.easymall.repository.TempUploadRepository;
import com.quocnva.easymall.service.review.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final TempUploadRepository tempUploadRepository;

    @Override
    @Transactional
    public ReviewResponse createReview(CreateReviewRequest request, String userEmail) {
        UserEntity user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        ProductEntity product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

        OrderEntity order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        // Validate: order phải thuộc user này
        if (!order.getUser().getUserId().equals(user.getUserId())) {
            throw new AppException(ErrorCode.REVIEW_ORDER_OWNERSHIP_DENIED);
        }

        // Validate: order phải ở trạng thái COMPLETED
        if (order.getOrderStatus() != OrderStatus.COMPLETED) {
            throw new AppException(ErrorCode.REVIEW_ORDER_NOT_COMPLETED);
        }

        // Validate: chưa review sản phẩm này trong đơn này
        boolean alreadyReviewed = reviewRepository.existsByUser_UserIdAndProduct_ProductIdAndOrder_OrderId(
                user.getUserId(), product.getProductId(), order.getOrderId());
        if (alreadyReviewed) {
            throw new AppException(ErrorCode.REVIEW_ALREADY_EXISTS);
        }

        ReviewEntity review = ReviewEntity.builder()
                .user(user)
                .product(product)
                .order(order)
                .rating(request.getRating())
                .comment(request.getComment())
                .reviewStatus(ReviewStatus.PENDING)
                .build();

        // Thêm ảnh nếu có
        if (request.getImageUrls() != null && !request.getImageUrls().isEmpty()) {
            List<ReviewImageEntity> images = request.getImageUrls().stream()
                    .map(url -> ReviewImageEntity.builder()
                            .review(review)
                            .imageUrl(url)
                            .build())
                    .collect(Collectors.toList());
            review.setImages(images);

            // Xóa bản ghi trung chuyển — ảnh đã được liên kết chính thức vào review
            request.getImageUrls().forEach(tempUploadRepository::deleteByUrl);
        }

        ReviewEntity saved = reviewRepository.save(review);
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReviewResponse> getProductReviews(Long productId, Pageable pageable) {
        return reviewRepository
                .findByProduct_ProductIdAndReviewStatus(productId, ReviewStatus.APPROVED, pageable)
                .map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public ReviewSummaryResponse getProductReviewSummary(Long productId) {
        long total = reviewRepository.countByProduct_ProductIdAndReviewStatus(productId, ReviewStatus.APPROVED);
        Double avg = reviewRepository.findAverageRatingByProductId(productId).orElse(null);

        // Build breakdown map {1:0, 2:0, 3:0, 4:0, 5:0} rồi điền vào từ query
        Map<Integer, Long> breakdown = new HashMap<>();
        for (int i = 1; i <= 5; i++) breakdown.put(i, 0L);

        reviewRepository.countByRatingForProduct(productId).forEach(row -> {
            Integer star = (Integer) row[0];
            Long count = (Long) row[1];
            breakdown.put(star, count);
        });

        return ReviewSummaryResponse.builder()
                .productId(productId)
                .averageRating(avg)
                .totalReviews(total)
                .ratingBreakdown(breakdown)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReviewResponse> getMyReviews(String userEmail, Pageable pageable) {
        UserEntity user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        return reviewRepository.findByUser_UserId(user.getUserId(), pageable).map(this::toResponse);
    }

    @Override
    @Transactional
    public ReviewResponse updateReviewStatus(Long reviewId, UpdateReviewStatusRequest request) {
        ReviewEntity review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new AppException(ErrorCode.REVIEW_NOT_FOUND));
        review.setReviewStatus(request.getStatus());
        return toResponse(reviewRepository.save(review));
    }

    @Override
    @Transactional
    public void deleteReview(Long reviewId, String userEmail) {
        ReviewEntity review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new AppException(ErrorCode.REVIEW_NOT_FOUND));

        UserEntity user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        // User chỉ được xóa review của chính mình (Admin bỏ qua check này qua @PreAuthorize)
        if (!review.getUser().getUserId().equals(user.getUserId())) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }

        reviewRepository.delete(review);
    }

    // ── Mapper nội bộ ──────────────────────────────────────────────────────

    private ReviewResponse toResponse(ReviewEntity review) {
        List<ReviewImageResponse> imageResponses = review.getImages().stream()
                .map(img -> ReviewImageResponse.builder()
                        .reviewImageId(img.getReviewImageId())
                        .imageUrl(img.getImageUrl())
                        .build())
                .collect(Collectors.toList());

        return ReviewResponse.builder()
                .reviewId(review.getReviewId())
                .orderId(review.getOrder() != null ? review.getOrder().getOrderId() : null)
                .userId(review.getUser() != null ? review.getUser().getUserId() : null)
                .userFullName(review.getUser() != null ? review.getUser().getFullName() : "Ẩn danh")
                .productId(review.getProduct().getProductId())
                .productName(review.getProduct().getProductName())
                .rating(review.getRating())
                .comment(review.getComment())
                .reviewStatus(review.getReviewStatus())
                .images(imageResponses)
                .createdAt(review.getCreatedAt())
                .build();
    }
}
