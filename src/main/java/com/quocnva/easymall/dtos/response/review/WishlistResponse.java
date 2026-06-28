package com.quocnva.easymall.dtos.response.review;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Getter
@Setter
@Builder
public class WishlistResponse {

    private Long wishlistId;
    private Long productId;
    private String productName;
    private String productSlug;

    /** Ảnh đầu tiên của sản phẩm (thumbnail) */
    private String thumbnailUrl;

    /** Giá thấp nhất trong các variants của sản phẩm */
    private BigDecimal minPrice;

    private Boolean inStock;
    private OffsetDateTime addedAt;
}
