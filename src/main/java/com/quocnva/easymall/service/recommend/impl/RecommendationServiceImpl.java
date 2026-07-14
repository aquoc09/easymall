package com.quocnva.easymall.service.recommend.impl;

import com.quocnva.easymall.dtos.response.product.ProductResponse;
import com.quocnva.easymall.entity.ProductEntity;
import com.quocnva.easymall.entity.ProductAssociationEntity;
import com.quocnva.easymall.entity.ProductSimilarityEntity;
import com.quocnva.easymall.entity.UserRecommendationEntity;
import com.quocnva.easymall.mapper.ProductMapper;
import com.quocnva.easymall.repository.ProductAssociationRepository;
import com.quocnva.easymall.repository.ProductRepository;
import com.quocnva.easymall.repository.ProductSimilarityRepository;
import com.quocnva.easymall.repository.UserRecommendationRepository;
import com.quocnva.easymall.service.recommend.RecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecommendationServiceImpl implements RecommendationService {

    private final UserRecommendationRepository userRecommendationRepository;
    private final ProductSimilarityRepository productSimilarityRepository;
    private final ProductAssociationRepository productAssociationRepository;
    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> getPersonalizedRecommendations(Long userId, int limit) {
        List<ProductEntity> products;

        if (userId == null) {
            products = getFallbackTrendingProducts();
        } else {
            List<UserRecommendationEntity> recommendations = userRecommendationRepository.findTopRecommendationsForUser(userId, limit);
            if (recommendations.isEmpty()) {
                products = getFallbackTrendingProducts();
            } else {
                products = recommendations.stream()
                        .map(UserRecommendationEntity::getProduct)
                        .filter(ProductEntity::getInStock)
                        .collect(Collectors.toList());
            }
        }

        return products.stream()
                .map(productMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> getSimilarProducts(Long productId, int limit) {
        List<ProductSimilarityEntity> similarities = productSimilarityRepository.findTopSimilarProducts(productId, limit);
        
        return similarities.stream()
                .map(ProductSimilarityEntity::getSimilarProduct)
                .filter(ProductEntity::getInStock)
                .map(productMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> getBoughtTogetherProducts(Long productId, int limit) {
        List<ProductAssociationEntity> associations = productAssociationRepository.findTopBoughtTogetherProducts(productId, limit);
        
        return associations.stream()
                .map(ProductAssociationEntity::getRelatedProduct)
                .filter(ProductEntity::getInStock)
                .map(productMapper::toResponse)
                .collect(Collectors.toList());
    }

    private List<ProductEntity> getFallbackTrendingProducts() {
        return productRepository.findTop10ByInStockTrueOrderBySoldCountDesc();
    }
}
