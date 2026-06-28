package com.quocnva.easymall.service.review;

import com.quocnva.easymall.dtos.response.review.WishlistResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface WishlistService {

    /**
     * Toggle: thêm vào wishlist nếu chưa có, bỏ ra nếu đã có.
     * @return true nếu đã thêm vào, false nếu vừa bỏ ra.
     */
    boolean toggleWishlist(Long productId, String userEmail);

    Page<WishlistResponse> getMyWishlist(String userEmail, Pageable pageable);

    void removeFromWishlist(Long productId, String userEmail);
}
