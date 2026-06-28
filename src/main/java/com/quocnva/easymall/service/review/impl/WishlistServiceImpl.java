package com.quocnva.easymall.service.review.impl;

import com.quocnva.easymall.dtos.response.review.WishlistResponse;
import com.quocnva.easymall.entity.ProductEntity;
import com.quocnva.easymall.entity.UserEntity;
import com.quocnva.easymall.entity.WishlistEntity;
import com.quocnva.easymall.exception.AppException;
import com.quocnva.easymall.exception.ErrorCode;
import com.quocnva.easymall.repository.ProductRepository;
import com.quocnva.easymall.repository.UserRepository;
import com.quocnva.easymall.repository.WishlistRepository;
import com.quocnva.easymall.service.review.WishlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class WishlistServiceImpl implements WishlistService {

    private final WishlistRepository wishlistRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    @Override
    @Transactional
    public boolean toggleWishlist(Long productId, String userEmail) {
        UserEntity user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        ProductEntity product = productRepository.findById(productId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

        Optional<WishlistEntity> existing = wishlistRepository
                .findByUser_UserIdAndProduct_ProductId(user.getUserId(), productId);

        if (existing.isPresent()) {
            wishlistRepository.delete(existing.get());
            return false; // đã bỏ ra
        } else {
            WishlistEntity wishlist = WishlistEntity.builder()
                    .user(user)
                    .product(product)
                    .build();
            wishlistRepository.save(wishlist);
            return true; // đã thêm vào
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<WishlistResponse> getMyWishlist(String userEmail, Pageable pageable) {
        UserEntity user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        return wishlistRepository.findByUser_UserId(user.getUserId(), pageable)
                .map(this::toResponse);
    }

    @Override
    @Transactional
    public void removeFromWishlist(Long productId, String userEmail) {
        UserEntity user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (!wishlistRepository.existsByUser_UserIdAndProduct_ProductId(user.getUserId(), productId)) {
            throw new AppException(ErrorCode.WISHLIST_ITEM_NOT_FOUND);
        }

        wishlistRepository.deleteByUser_UserIdAndProduct_ProductId(user.getUserId(), productId);
    }

    // ── Mapper nội bộ ──────────────────────────────────────────────────────

    private WishlistResponse toResponse(WishlistEntity entity) {
        ProductEntity product = entity.getProduct();

        // Lấy giá thấp nhất từ các variants
        java.math.BigDecimal minPrice = product.getVariants().stream()
                .filter(v -> Boolean.TRUE.equals(v.getIsActive()))
                .map(v -> v.getPrice())
                .min(java.math.BigDecimal::compareTo)
                .orElse(null);

        // Lấy ảnh đầu tiên làm thumbnail
        String thumbnail = product.getImages().stream()
                .findFirst()
                .map(img -> img.getImageUrl())
                .orElse(null);

        return WishlistResponse.builder()
                .wishlistId(entity.getWishlistId())
                .productId(product.getProductId())
                .productName(product.getProductName())
                .productSlug(product.getProductSlug())
                .thumbnailUrl(thumbnail)
                .minPrice(minPrice)
                .inStock(product.getInStock())
                .addedAt(entity.getCreatedAt())
                .build();
    }
}
