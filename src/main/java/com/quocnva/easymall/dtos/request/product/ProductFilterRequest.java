package com.quocnva.easymall.dtos.request.product;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductFilterRequest {

    /** Search term for product name or slug */
    private String keyword;

    /** Fetch products from this category and its children */
    private String categoryCode;

    /** Special collection: NEW_ARRIVALS, BEST_SELLERS, POPULAR */
    private String collection;

    private BigDecimal minPrice;
    private BigDecimal maxPrice;

    /** Minimum average rating */
    private BigDecimal minRating;

    /** 0: Nữ, 1: Nam, 2: Unisex */
    private Short targetGender;

    private Boolean inStock;

    private Boolean inPopular;
}
