package com.quocnva.easymall.service.tracking.impl;

import com.quocnva.easymall.dtos.request.tracking.TrackingEventRequest;
import com.quocnva.easymall.entity.UserBehaviorEntity;
import com.quocnva.easymall.repository.CategoryRepository;
import com.quocnva.easymall.repository.ProductRepository;
import com.quocnva.easymall.repository.UserBehaviorRepository;
import com.quocnva.easymall.repository.UserRepository;
import com.quocnva.easymall.service.tracking.TrackingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class TrackingServiceImpl implements TrackingService {

    private final UserBehaviorRepository userBehaviorRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    @Async
    @Override
    public void trackEvent(TrackingEventRequest request) {
        try {
            UserBehaviorEntity behavior = UserBehaviorEntity.builder()
                    .sessionId(request.getSessionId())
                    .actionType(request.getActionType())
                    .keyword(request.getKeyword())
                    .contextData(request.getContextData())
                    .variantId(request.getVariantId())
                    .durationSeconds(request.getDurationSeconds())
                    .source(request.getSource())
                    .build();

            // Using getReferenceById to avoid unnecessary SELECT queries
            if (request.getUserId() != null) {
                behavior.setUser(userRepository.getReferenceById(request.getUserId()));
            }
            if (request.getProductId() != null) {
                behavior.setProduct(productRepository.getReferenceById(request.getProductId()));
            }
            if (request.getCategoryId() != null) {
                behavior.setCategory(categoryRepository.getReferenceById(request.getCategoryId()));
            }

            userBehaviorRepository.save(behavior);
        } catch (Exception e) {
            // Log error but do not throw, we don't want to break the async flow
            log.error("Failed to save tracking event for session {}: {}", request.getSessionId(), e.getMessage());
        }
    }
}
