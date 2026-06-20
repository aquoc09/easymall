package com.quocnva.easymall.mapper;

import com.quocnva.easymall.dtos.request.category.CategoryCreateRequest;
import com.quocnva.easymall.dtos.request.category.CategoryUpdateRequest;
import com.quocnva.easymall.dtos.response.category.CategoryResponse;
import com.quocnva.easymall.entity.CategoryEntity;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

@Component
public class CategoryMapper {

    public CategoryEntity toEntity(CategoryCreateRequest request) {
        if (request == null) {
            return null;
        }

        return CategoryEntity.builder()
                .categoryName(request.getCategoryName())
                .parentId(request.getParentId())
                .iconUrl(request.getIconUrl())
                .targetDemographic(request.getTargetDemographic())
                .categoryType(request.getCategoryType())
                .displayOrder(request.getDisplayOrder())
                .build();
    }

    public void updateEntityFromRequest(CategoryUpdateRequest request, CategoryEntity entity) {
        if (request == null || entity == null) {
            return;
        }

        if (request.getCategoryName() != null) {
            entity.setCategoryName(request.getCategoryName());
        }
        if (request.getCategoryStatus() != null) {
            entity.setCategoryStatus(request.getCategoryStatus());
        }
        if (request.getIconUrl() != null) {
            entity.setIconUrl(request.getIconUrl());
        }
        if (request.getTargetDemographic() != null) {
            entity.setTargetDemographic(request.getTargetDemographic());
        }
        if (request.getCategoryType() != null) {
            entity.setCategoryType(request.getCategoryType());
        }
        if (request.getDisplayOrder() != null) {
            entity.setDisplayOrder(request.getDisplayOrder());
        }
    }

    public CategoryResponse toResponse(CategoryEntity entity) {
        if (entity == null) {
            return null;
        }

        return CategoryResponse.builder()
                .categoryId(entity.getCategoryId())
                .categoryCode(entity.getCategoryCode())
                .categoryName(entity.getCategoryName())
                .categoryStatus(entity.getCategoryStatus())
                .level(entity.getLevel())
                .parentId(entity.getParentId())
                .iconUrl(entity.getIconUrl())
                .targetDemographic(entity.getTargetDemographic())
                .categoryType(entity.getCategoryType())
                .displayOrder(entity.getDisplayOrder())
                .children(new ArrayList<>())
                .build();
    }
}
