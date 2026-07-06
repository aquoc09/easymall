package com.quocnva.easymall.service.product.impl;

import com.quocnva.easymall.repository.ProductRepository;
import com.quocnva.easymall.service.product.ProductStatsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * ProductStatsServiceImpl — cập nhật stats denormalized qua Native Query.
 * Không dùng entity.set() để tránh Hibernate dirty checking và @UpdateTimestamp
 * trigger không cần thiết cho các lần update stat.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProductStatsServiceImpl implements ProductStatsService {

    private final ProductRepository productRepository;

    @Override
    @Transactional
    public void incrementViewCount(Long productId) {
        productRepository.incrementViewCount(productId);
        log.debug("[ProductStats] Incremented view_count for product #{}", productId);
    }

    @Override
    @Transactional
    public void updateRatingStats(Long productId) {
        productRepository.recalculateRatingStats(productId);
        log.debug("[ProductStats] Recalculated rating stats for product #{}", productId);
    }

    @Override
    @Transactional
    public void updateSoldCount(Long productId, int quantity) {
        productRepository.increaseSoldCount(productId, quantity);
        log.debug("[ProductStats] Increased sold_count by {} for product #{}", quantity, productId);
    }
}
