package com.quocnva.easymall.mapper;

import com.quocnva.easymall.dtos.request.category.CategoryCreateRequest;
import com.quocnva.easymall.dtos.request.category.CategoryUpdateRequest;
import com.quocnva.easymall.dtos.response.category.CategoryResponse;
import com.quocnva.easymall.entity.CategoryEntity;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;

import java.util.ArrayList;

@Component
public class CategoryMapper {

    @Value("${storage.base-url}")
    private String storageBaseUrl;

    /**
     * Cast Integer → Short an toàn (DB schema dùng SMALLINT = int2).
     * DTO vẫn giữ Integer để API consumer không bị phá vỡ.
     */
    private Short toShort(Integer value) {
        return value == null ? null : value.shortValue();
    }

    public CategoryEntity toEntity(CategoryCreateRequest request) {
        if (request == null) return null;

        return CategoryEntity.builder()
                .categoryName(request.getCategoryName())
                .parentId(request.getParentId())
                .iconUrl(request.getIconUrl())
                .displayOrder(request.getDisplayOrder())
                .build();
    }

    public void updateEntityFromRequest(CategoryUpdateRequest request, CategoryEntity entity) {
        if (request == null || entity == null) return;

        if (request.getCategoryName() != null)   entity.setCategoryName(request.getCategoryName());
        if (request.getCategoryStatus() != null) entity.setCategoryStatus(toShort(request.getCategoryStatus()));
        if (request.getIconUrl() != null)        entity.setIconUrl(request.getIconUrl());
        if (request.getDisplayOrder() != null)   entity.setDisplayOrder(request.getDisplayOrder());
    }

    public CategoryResponse toResponse(CategoryEntity entity) {
        if (entity == null) return null;

        String fullIconUrl = entity.getIconUrl();
        if (fullIconUrl != null && !fullIconUrl.startsWith("http")) {
            fullIconUrl = storageBaseUrl + "/" + fullIconUrl;
        }

        return CategoryResponse.builder()
                .categoryId(entity.getCategoryId())
                .categoryCode(entity.getCategoryCode())
                .categoryName(entity.getCategoryName())
                .categoryStatus(entity.getCategoryStatus() != null ? entity.getCategoryStatus().intValue() : null)
                .level(entity.getLevel())
                .parentId(entity.getParentId())
                .iconUrl(fullIconUrl)
                .displayOrder(entity.getDisplayOrder())
                .updatedAt(entity.getUpdatedAt())
                .children(new ArrayList<>())
                .build();
    }
}
