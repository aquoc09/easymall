package com.quocnva.easymall.controller;

import com.quocnva.easymall.dtos.response.ApiResponse;
import com.quocnva.easymall.dtos.response.product.ProductResponse;
import com.quocnva.easymall.service.recommend.RecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/recommendations")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;

    @GetMapping("/for-you")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getPersonalizedRecommendations(
            @RequestParam(required = false) Long userId,
            @RequestParam(defaultValue = "10") int limit) {
        
        List<ProductResponse> products = recommendationService.getPersonalizedRecommendations(userId, limit);
        return ResponseEntity.ok(ApiResponse.<List<ProductResponse>>builder()
                .result(products)
                .build());
    }

    @GetMapping("/products/{id}/similar")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getSimilarProducts(
            @PathVariable Long id,
            @RequestParam(defaultValue = "10") int limit) {
        
        List<ProductResponse> products = recommendationService.getSimilarProducts(id, limit);
        return ResponseEntity.ok(ApiResponse.<List<ProductResponse>>builder()
                .result(products)
                .build());
    }

    @GetMapping("/products/{id}/bought-together")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getBoughtTogetherProducts(
            @PathVariable Long id,
            @RequestParam(defaultValue = "5") int limit) {
        
        List<ProductResponse> products = recommendationService.getBoughtTogetherProducts(id, limit);
        return ResponseEntity.ok(ApiResponse.<List<ProductResponse>>builder()
                .result(products)
                .build());
    }
}
