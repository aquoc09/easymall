package com.quocnva.easymall.service;

import com.quocnva.easymall.dtos.response.product.ProductResponse;

import java.util.List;

public interface RecommendationService {
    List<ProductResponse> getPersonalizedRecommendations(Long userId, int limit);
    List<ProductResponse> getSimilarProducts(Long productId, int limit);
    List<ProductResponse> getBoughtTogetherProducts(Long productId, int limit);
}
